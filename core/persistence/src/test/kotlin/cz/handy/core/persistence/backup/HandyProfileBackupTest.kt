package cz.handy.core.persistence.backup

import cz.handy.core.common.voice.EmbeddingFloatCodec
import cz.handy.core.common.voice.VoiceEmbeddingDimensions
import kotlin.test.Test
import kotlin.test.assertEquals

class HandyProfileBackupTest {
    @Test
    fun sealAndOpen_roundtrip() {
        val plain =
            HandyProfilePlainV1(
                embeddingB64 =
                    EmbeddingFloatCodec.encodeToString(
                        FloatArray(VoiceEmbeddingDimensions.ECAPA_V1) { i -> (i % 7) * 0.01f },
                    ),
                aliases =
                    listOf(
                        HandyProfilePlainV1.ProfileAliasEntryV1(
                            aliasKey = "bratr",
                            targetContact = "Jan",
                            createdAtEpochMs = 100L,
                        ),
                    ),
                embeddingVersions =
                    listOf(
                        HandyProfilePlainV1.ProfileEmbeddingVersionEntryV1(
                            schemaVersion = 1,
                            modelLabel = "ecapa",
                            vectorDim = VoiceEmbeddingDimensions.ECAPA_V1,
                            savedAtEpochMs = 200L,
                            notes = null,
                        ),
                    ),
                betaFeedback = emptyList(),
            )
        val password = "testSecret8".toCharArray()
        try {
            val file = HandyProfileBackup.seal(plain, password)
            val back = HandyProfileBackup.open(file, password).getOrThrow()
            assertEquals(plain.embeddingB64, back.embeddingB64)
            assertEquals(plain.aliases.size, back.aliases.size)
            assertEquals("bratr", back.aliases.first().aliasKey)
            assertEquals(plain.embeddingVersions.size, back.embeddingVersions.size)
            assertEquals(plain.betaFeedback, back.betaFeedback)
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun json_roundtrip_withoutCrypto() {
        val plain =
            HandyProfilePlainV1(
                embeddingB64 = null,
                aliases = emptyList(),
                embeddingVersions = emptyList(),
                betaFeedback = null,
            )
        val json = HandyProfileBackup.toJson(plain)
        val parsed = HandyProfileBackup.fromJson(json).getOrThrow()
        assertEquals(null, parsed.embeddingB64)
        assertEquals(0, parsed.aliases.size)
        assertEquals(null, parsed.betaFeedback)
    }

    @Test
    fun json_roundtrip_betaFeedback_emptyMeansReplaceWithZeroRows() {
        val plain =
            HandyProfilePlainV1(
                embeddingB64 = null,
                aliases = emptyList(),
                embeddingVersions = emptyList(),
                betaFeedback = emptyList(),
            )
        val parsed = HandyProfileBackup.fromJson(HandyProfileBackup.toJson(plain)).getOrThrow()
        requireNotNull(parsed.betaFeedback)
        assertEquals(0, parsed.betaFeedback.size)
    }

    @Test
    fun sealAndOpen_betaFeedbackEntries() {
        val plain =
            HandyProfilePlainV1(
                embeddingB64 = null,
                aliases = emptyList(),
                embeddingVersions = emptyList(),
                betaFeedback =
                    listOf(
                        HandyProfilePlainV1.ProfileBetaFeedbackEntryV1(
                            createdAtEpochMs = 900L,
                            satisfactionStars = 5,
                            messageText = "Vše pohoda",
                        ),
                    ),
            )
        val pwd = "longPassPhr".toCharArray()

        try {
            val blob = HandyProfileBackup.seal(plain, pwd)
            val back = HandyProfileBackup.open(blob, pwd).getOrThrow()
            requireNotNull(back.betaFeedback)

            assertEquals(1, back.betaFeedback.size)
            assertEquals("Vše pohoda", back.betaFeedback.single().messageText)

            assertEquals(5, back.betaFeedback.single().satisfactionStars)
        } finally {
            pwd.fill('\u0000')
        }
    }
}
