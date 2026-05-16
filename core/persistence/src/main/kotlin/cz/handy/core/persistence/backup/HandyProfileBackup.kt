package cz.handy.core.persistence.backup

import cz.handy.core.common.voice.EmbeddingFloatCodec
import cz.handy.core.common.voice.VoiceEmbeddingDimensions
import cz.handy.core.persistence.ContactAliasStore
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Jasná data profilu (před šifrováním, UTF-8 JSON) — embedding jen jako Base64 řetězec z prefs ([F4-T03]).
 *
 * @property betaFeedback pokud `null`, import zachová stávající řádky v `beta_feedback` (starší zálohy bez klíče).
 */
data class HandyProfilePlainV1(
    val embeddingB64: String?,
    val aliases: List<ProfileAliasEntryV1>,
    val embeddingVersions: List<ProfileEmbeddingVersionEntryV1>,
    val betaFeedback: List<ProfileBetaFeedbackEntryV1>?,
) {
    data class ProfileAliasEntryV1(
        val aliasKey: String,
        val targetContact: String,
        val createdAtEpochMs: Long,
    )

    data class ProfileEmbeddingVersionEntryV1(
        val schemaVersion: Int,
        val modelLabel: String,
        val vectorDim: Int,
        val savedAtEpochMs: Long,
        val notes: String?,
    )

    data class ProfileBetaFeedbackEntryV1(
        val createdAtEpochMs: Long,
        val satisfactionStars: Int,
        val messageText: String,
    )
}

/**
 * AES-GCM soubor s měkkým heslem (PBKDF2); žádný cloud.
 */
object HandyProfileBackup {
    const val PLAIN_FORMAT_VERSION: Int = 1

    private const val MAX_BETA_FEEDBACK_IN_BACKUP = 256

    private const val MAX_BETA_MESSAGE_CHARS = 8192
    private const val FILE_MAGIC = "HANDYPF1"
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val GCM_TAG_BITS = 128
    private const val AES_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000

    fun toJson(plain: HandyProfilePlainV1): String {
        val root = JSONObject()
        root.put("format", PLAIN_FORMAT_VERSION)
        if (plain.embeddingB64 != null) {
            root.put("embeddingB64", plain.embeddingB64)
        } else {
            root.put("embeddingB64", JSONObject.NULL)
        }
        val aliasArr = JSONArray()
        plain.aliases.forEach { a ->
            aliasArr.put(
                JSONObject().apply {
                    put("aliasKey", a.aliasKey)
                    put("targetContact", a.targetContact)
                    put("createdAtEpochMs", a.createdAtEpochMs)
                },
            )
        }
        root.put("aliases", aliasArr)
        val verArr = JSONArray()
        plain.embeddingVersions.forEach { v ->
            verArr.put(
                JSONObject().apply {
                    put("schemaVersion", v.schemaVersion)
                    put("modelLabel", v.modelLabel)
                    put("vectorDim", v.vectorDim)
                    put("savedAtEpochMs", v.savedAtEpochMs)
                    put("notes", v.notes ?: JSONObject.NULL)
                },
            )
        }
        root.put("embeddingVersions", verArr)
        if (plain.betaFeedback != null) {
            val bf = JSONArray()
            plain.betaFeedback.forEach { b ->
                bf.put(
                    JSONObject().apply {
                        put("createdAtEpochMs", b.createdAtEpochMs)
                        put("satisfactionStars", b.satisfactionStars)
                        put("messageText", b.messageText)
                    },
                )
            }
            root.put("betaFeedback", bf)
        }
        return root.toString()
    }

    fun fromJson(jsonText: String): Result<HandyProfilePlainV1> =
        runCatching {
            val root = JSONObject(jsonText)
            val fmt = root.getInt("format")
            require(fmt == PLAIN_FORMAT_VERSION) {
                "Nepodporovaná verze zálohy: $fmt"
            }
            val emb =
                if (root.isNull("embeddingB64")) {
                    null
                } else {
                    root.getString("embeddingB64").trim().ifBlank { null }
                }
            emb?.let { validateEmbeddingBase64(it) }
            val aliases = mutableListOf<HandyProfilePlainV1.ProfileAliasEntryV1>()
            val aliasJa = root.getJSONArray("aliases")
            require(aliasJa.length() <= ContactAliasStore.MAX_CONTACT_ALIASES) {
                "Záloha obsahuje příliš mnoho aliasů (${aliasJa.length()})."
            }
            for (i in 0 until aliasJa.length()) {
                val o = aliasJa.getJSONObject(i)
                val k = o.getString("aliasKey").trim().lowercase()
                val t = o.getString("targetContact").trim()
                require(k.isNotEmpty() && t.isNotEmpty()) { "Prázdný alias nebo cíl v záloze." }
                aliases.add(
                    HandyProfilePlainV1.ProfileAliasEntryV1(
                        aliasKey = k,
                        targetContact = t,
                        createdAtEpochMs = o.getLong("createdAtEpochMs"),
                    ),
                )
            }
            val vers = mutableListOf<HandyProfilePlainV1.ProfileEmbeddingVersionEntryV1>()
            val verJa = root.getJSONArray("embeddingVersions")
            for (i in 0 until verJa.length()) {
                val o = verJa.getJSONObject(i)
                vers.add(
                    HandyProfilePlainV1.ProfileEmbeddingVersionEntryV1(
                        schemaVersion = o.getInt("schemaVersion"),
                        modelLabel = o.getString("modelLabel"),
                        vectorDim = o.getInt("vectorDim"),
                        savedAtEpochMs = o.getLong("savedAtEpochMs"),
                        notes =
                            if (o.isNull("notes")) {
                                null
                            } else {
                                o.getString("notes")
                            },
                    ),
                )
            }
            val betaFeedback: List<HandyProfilePlainV1.ProfileBetaFeedbackEntryV1>? =
                if (!root.has("betaFeedback")) {
                    null
                } else {
                    val fbJa = root.getJSONArray("betaFeedback")
                    require(fbJa.length() <= MAX_BETA_FEEDBACK_IN_BACKUP) {
                        "Záloha obsahuje příliš mnoho řádků zpětné vazby (${fbJa.length()})."
                    }
                    val fbList = mutableListOf<HandyProfilePlainV1.ProfileBetaFeedbackEntryV1>()
                    for (i in 0 until fbJa.length()) {
                        val o = fbJa.getJSONObject(i)
                        val msg = o.getString("messageText").trim()
                        require(msg.length <= MAX_BETA_MESSAGE_CHARS) {
                            "Příliš dlouhý text zpětné vazby v záloze."
                        }
                        val stars = o.getInt("satisfactionStars").coerceIn(1, 5)
                        require(msg.isNotEmpty()) { "Prázdný text zpětné vazby v záloze." }
                        fbList.add(
                            HandyProfilePlainV1.ProfileBetaFeedbackEntryV1(
                                createdAtEpochMs = o.getLong("createdAtEpochMs"),
                                satisfactionStars = stars,
                                messageText = msg,
                            ),
                        )
                    }
                    fbList
                }
            HandyProfilePlainV1(
                embeddingB64 = emb,
                aliases = aliases,
                embeddingVersions = vers,
                betaFeedback = betaFeedback,
            )
        }

    fun validateEmbeddingBase64(b64: String) {
        val vec = EmbeddingFloatCodec.decodeFromBase64(b64)
        require(vec.size == VoiceEmbeddingDimensions.ECAPA_V1) {
            "Neplatný rozměr embeddingu: ${vec.size}"
        }
    }

    fun seal(
        plain: HandyProfilePlainV1,
        password: CharArray,
    ): ByteArray {
        require(password.isNotEmpty()) { "Heslo nesmí být prázdné." }
        val plaintext = toJson(plain).toByteArray(StandardCharsets.UTF_8)
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        val ciphertext = cipher.doFinal(plaintext)
        val magic = FILE_MAGIC.toByteArray(StandardCharsets.US_ASCII)
        require(magic.size == 8) { "MAGIC length" }
        return ByteBuffer
            .allocate(magic.size + salt.size + iv.size + ciphertext.size)
            .put(magic)
            .put(salt)
            .put(iv)
            .put(ciphertext)
            .array()
    }

    fun open(
        fileBytes: ByteArray,
        password: CharArray,
    ): Result<HandyProfilePlainV1> =
        runCatching {
            require(password.isNotEmpty())
            require(fileBytes.size >= 8 + SALT_BYTES + IV_BYTES + 16) { "Soubor je příliš krátký." }
            val bb = ByteBuffer.wrap(fileBytes)
            val magic = ByteArray(8)
            bb.get(magic)
            require(
                Arrays.equals(
                    magic,
                    FILE_MAGIC.toByteArray(StandardCharsets.US_ASCII),
                ),
            ) {
                "Neznámý formát souboru (není záloha Handy?)."
            }
            val salt = ByteArray(SALT_BYTES)
            bb.get(salt)
            val iv = ByteArray(IV_BYTES)
            bb.get(iv)
            val ciphertext = ByteArray(bb.remaining())
            bb.get(ciphertext)
            val key = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
            val plainBytes = cipher.doFinal(ciphertext)
            val json = String(plainBytes, StandardCharsets.UTF_8)
            fromJson(json).getOrThrow()
        }

    private fun deriveKey(
        password: CharArray,
        salt: ByteArray,
    ): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec: KeySpec =
            PBEKeySpec(password, salt, PBKDF2_ITERATIONS, AES_BITS)
        val tmp = factory.generateSecret(spec).encoded
        return SecretKeySpec(tmp, "AES")
    }
}
