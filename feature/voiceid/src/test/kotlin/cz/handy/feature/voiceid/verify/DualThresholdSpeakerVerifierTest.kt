package cz.handy.feature.voiceid.verify

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DualThresholdSpeakerVerifierTest {
    @Test
    fun identicalEmbeddingCosineOne() {
        val e = FloatArray(192) { if (it == 0) 1f else 0f }
        val c = DualThresholdSpeakerVerifier.cosineSimilarity(e, e)
        assertEquals(1f, c, absoluteTolerance = 1e-5f)
    }

    @Test
    fun orthogonalCosineZero() {
        val a = FloatArray(192) { if (it == 0) 1f else 0f }
        val b = FloatArray(192) { if (it == 1) 1f else 0f }
        val c = DualThresholdSpeakerVerifier.cosineSimilarity(a, b)
        assertEquals(0f, c, absoluteTolerance = 1e-5f)
    }

    @Test
    fun verdictBands() {
        val t = VerificationThresholds(0.78f, 0.65f)
        assertEquals(VerificationVerdict.StrongAccept, DualThresholdSpeakerVerifier.verdict(0.9f, t))
        assertEquals(VerificationVerdict.Uncertain, DualThresholdSpeakerVerifier.verdict(0.7f, t))
        assertEquals(VerificationVerdict.Reject, DualThresholdSpeakerVerifier.verdict(0.5f, t))
    }

    @Test
    fun verdictInclusiveHighExclusiveLowBoundary() {
        val t = VerificationThresholds(0.78f, 0.65f)
        assertEquals(VerificationVerdict.StrongAccept, DualThresholdSpeakerVerifier.verdict(0.78f, t))
        assertEquals(VerificationVerdict.Uncertain, DualThresholdSpeakerVerifier.verdict(0.779f, t))
        assertEquals(VerificationVerdict.Uncertain, DualThresholdSpeakerVerifier.verdict(0.65f, t))
        assertEquals(VerificationVerdict.Reject, DualThresholdSpeakerVerifier.verdict(0.649f, t))
    }

    @Test
    fun thresholdOrderEnforced() {
        assertFailsWith<IllegalArgumentException> {
            VerificationThresholds(cosineHigh = 0.5f, cosineLow = 0.8f)
        }
    }
}
