package cz.handy.feature.voiceid.verify

import cz.handy.feature.voiceid.ecapa.SpeechbrainEcapaPreprocessor

/**
 * Kosínová podobnost L2‑normalizovaných ECAPA embeddingů ([F1‑T04]).
 * Pro vektory na jednotkové kouli je **cos = skalární součin**.
 */
object DualThresholdSpeakerVerifier {
    fun cosineSimilarity(
        a: FloatArray,
        b: FloatArray,
        expectedDim: Int = SpeechbrainEcapaPreprocessor.EMBEDDING_DIM,
    ): Float {
        require(a.size == expectedDim && b.size == expectedDim) {
            "Embedding dim mismatch: ${a.size} vs ${b.size} (expected $expectedDim)."
        }
        var s = 0.0
        for (i in a.indices) {
            s += (a[i] * b[i]).toDouble()
        }
        return s.toFloat().coerceIn(-1f, 1f)
    }

    fun verdict(
        cosineScore: Float,
        thresholds: VerificationThresholds,
    ): VerificationVerdict =
        when {
            cosineScore >= thresholds.cosineHigh -> VerificationVerdict.StrongAccept
            cosineScore < thresholds.cosineLow -> VerificationVerdict.Reject
            else -> VerificationVerdict.Uncertain
        }

    fun verify(
        sample: FloatArray,
        reference: FloatArray,
        thresholds: VerificationThresholds,
    ): Pair<Float, VerificationVerdict> {
        val score = cosineSimilarity(sample, reference, thresholds.embeddingDim)
        return score to verdict(score, thresholds)
    }
}
