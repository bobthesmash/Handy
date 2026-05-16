package cz.handy.feature.voiceid.verify

import cz.handy.feature.voiceid.ecapa.SpeechbrainEcapaPreprocessor

/** Uživatelsky nastavitelné mezní kosínové skóre ([F1‑T04]) + práh anti-spoof ([F5‑T02]). */
data class VerificationThresholds(
    val cosineHigh: Float = DEFAULT_COSINE_HIGH,
    val cosineLow: Float = DEFAULT_COSINE_LOW,
    val antiSpoofRejectAbove: Float = DEFAULT_ANTI_SPOOF_REJECT_ABOVE,
    val embeddingDim: Int = SpeechbrainEcapaPreprocessor.EMBEDDING_DIM,
) {
    init {
        require(cosineLow in -1f..1f && cosineHigh in -1f..1f) {
            "Thresholds must lie in [-1,1] (cosine similarity domain)."
        }
        require(cosineLow <= cosineHigh) {
            "T_low must be ≤ T_high (got low=$cosineLow high=$cosineHigh)."
        }
        require(antiSpoofRejectAbove in 0f..1f) {
            "Anti-spoof reject threshold must lie in [0,1] (P(spoof) domain)."
        }
    }

    companion object {
        const val DEFAULT_COSINE_HIGH = 0.78f
        const val DEFAULT_COSINE_LOW = 0.65f

        /** P(spoof) nad tímto prahem znamená zamítnutí tahu při přítomnosti `anti_spoof.onnx`. */
        const val DEFAULT_ANTI_SPOOF_REJECT_ABOVE = 0.5f
    }
}

enum class VerificationVerdict {
    /** ≥ T_high — spolehlivý majitel. */
    StrongAccept,

    /**
     * Kosínová podobnost v \[T_low, T_high) (hranice viz [DualThresholdSpeakerVerifier]).
     *
     * Produktová politika slabého signálu (audit C‑03): **nedispatchovat** kritickou akci ani NLU řetězec na nejistém skóre.
     * - Brána před NLU (`HandyAssistantViewModel.completeSpeakerPhraseGateOrAbort`): TTS výzva k opakování příkazu,
     *   návrat do idle přes `onVerifyFailed()` — uživatel musí říct celý tah znovu výrazněji.
     * - Druhý krok u destruktivních intentů ([DestructiveConfirmVoiceVerifier]): také zamítnuto, dokud nevznikne ≥ T_high.
     */
    Uncertain,

    /** < T_low — odmítnout. */
    Reject,
}
