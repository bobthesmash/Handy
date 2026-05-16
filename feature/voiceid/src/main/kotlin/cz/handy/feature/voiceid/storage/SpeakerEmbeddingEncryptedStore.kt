package cz.handy.feature.voiceid.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import cz.handy.core.common.voice.EmbeddingFloatCodec
import cz.handy.core.common.voice.VoiceEmbeddingDimensions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ukládá primární L2‑normalizovaný embedding majitele (**192‑D ECAPA**) v
 * **[EncryptedSharedPreferences]** a umožňuje **adaptivní EMA aktualizaci** po každé nové vzorce ([F1‑T03]).
 */
class SpeakerEmbeddingEncryptedStore internal constructor(
    private val prefs: SharedPreferences,
    /** Váha nového vektoru v EMA při `[adaptiveMerge]`: `µ←(1‑α)·µ+α·x`, pak L2. */
    private val adaptiveBlendAlpha: Float = DEFAULT_ADAPTIVE_ALPHA,
    private val expectedDim: Int = VoiceEmbeddingDimensions.ECAPA_V1,
) {
    constructor(
        context: Context,
        adaptiveBlendAlpha: Float = DEFAULT_ADAPTIVE_ALPHA,
    ) : this(createEncryptedPrefs(context), adaptiveBlendAlpha)

    private val lock = Any()

    private val _embedding =
        MutableStateFlow<FloatArray?>(
            runCatching { loadFromDisk() }.getOrElse { null },
        )

    /** Aktuálně uložený vektor pouze lokálním procesem (bez reaktivních listenerů pro cizí edity). */
    val embedding: StateFlow<FloatArray?> = _embedding.asStateFlow()

    fun peekSynchronized(): FloatArray? =
        synchronized(lock) {
            _embedding.value?.copyOf()
        }

    /**
     * Přímá náhrada (např. plná re‑enrollment dávka bez průměrování napříč dřívějším profilem).
     */
    fun replaceAll(embedding: FloatArray): Result<Unit> {
        checkDim(embedding)
        val copy =
            embedding.copyOf().also {
                EmbeddingFloatCodec.l2NormalizeInPlace(it)
            }
        synchronized(lock) {
            return runCatching {
                persist(copy)
                _embedding.value = copy
            }
        }
    }

    /**
     * **Rolling adaptive update**: pokud embedding neexistuje, uloží se `candidate`;
     * jinak EMA viz [adaptiveBlendAlpha], poté **L2** normalizace.
     */
    fun adaptiveMerge(candidate: FloatArray): Result<Unit> {
        checkDim(candidate)
        val cand =
            candidate.copyOf().also {
                EmbeddingFloatCodec.l2NormalizeInPlace(it)
            }
        synchronized(lock) {
            return runCatching {
                val current = _embedding.value ?: loadFromDisk()
                val blended =
                    if (current == null) {
                        cand
                    } else {
                        blending(current, cand, adaptiveBlendAlpha).also {
                            EmbeddingFloatCodec.l2NormalizeInPlace(it)
                        }
                    }
                persist(blended)
                _embedding.value = blended
            }
        }
    }

    /**
     * Syrový Base64 centroidu ve stejném formátu jako v šifrovaných prefs — pro export zálohy ([F4-T03]).
     */
    fun exportEmbeddingBase64ForBackup(): String? =
        synchronized(lock) {
            prefs.getString(KEY_EMBEDDING_B64_V1, null)
        }

    /**
     * Obnova centroidu ze zálohy (validace rozměru + L2 jako u [replaceAll]).
     */
    fun importEmbeddingFromBackupBase64(encoded: String): Result<Unit> {
        val arr =
            runCatching { EmbeddingFloatCodec.decodeFromBase64(encoded) }
                .getOrElse { return Result.failure(it) }
        return replaceAll(arr)
    }

    fun clear() {
        synchronized(lock) {
            check(
                prefs
                    .edit()
                    .remove(KEY_EMBEDDING_B64_V1)
                    .remove(KEY_SCHEMA_VER)
                    .commit(),
            ) {
                "Failed to clear speaker embedding prefs."
            }
            _embedding.value = null
        }
    }

    /** Zda byl uložen alespoň jeden centroid. */
    fun hasSpeakerProfile(): Boolean =
        synchronized(lock) {
            prefs.contains(KEY_EMBEDDING_B64_V1)
        }

    private fun blending(
        baseline: FloatArray,
        newcomer: FloatArray,
        alpha: Float,
    ): FloatArray {
        val beta = (1f - alpha).coerceIn(0f, 1f)
        val a = alpha.coerceIn(0f, 1f)
        return FloatArray(baseline.size) { i ->
            beta * baseline[i] + a * newcomer[i]
        }
    }

    private fun persist(embeddingVec: FloatArray) {
        val encoded = EmbeddingFloatCodec.encodeToString(embeddingVec)
        val ok =
            prefs
                .edit()
                .putString(KEY_EMBEDDING_B64_V1, encoded)
                .putInt(KEY_SCHEMA_VER, SCHEMA_V)
                .commit()
        check(ok) {
            "Failed to persist speaker embedding (EncryptedSharedPreferences commit=false)."
        }
    }

    private fun loadFromDisk(): FloatArray? {
        val enc = prefs.getString(KEY_EMBEDDING_B64_V1, null) ?: return null
        return EmbeddingFloatCodec.decodeFromBase64(enc)
    }

    private fun checkDim(v: FloatArray) {
        require(v.size == expectedDim) {
            "Embedding dimension ${v.size}, expected $expectedDim."
        }
    }

    private companion object {
        const val PREFS_NAME = "handy_voice_owner_profile"
        const val KEY_EMBEDDING_B64_V1 = "embed_centroid_le_b64_v1"
        const val KEY_SCHEMA_VER = "embed_schema_ver"
        const val SCHEMA_V = 1
        const val DEFAULT_ADAPTIVE_ALPHA = 0.18f

        fun createEncryptedPrefs(context: Context): SharedPreferences {
            val app = context.applicationContext

            @Suppress("DEPRECATION")
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

            @Suppress("DEPRECATION")
            return EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                app,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
