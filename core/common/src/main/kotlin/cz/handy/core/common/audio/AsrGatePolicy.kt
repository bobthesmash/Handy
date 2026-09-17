package cz.handy.core.common.audio

/**
 * Hot-tunable ASR confidence policy. Default [AUTO] treats scores outside `[0, 1]` as
 * log-probabilities (Sherpa `ysProbs` on device) so the built-in gate works without a JSON file.
 */
enum class AsrGateMode {
    AUTO,
    PROB,
    LOGPROB,
    OFF,
    ;

    companion object {
        fun fromWire(raw: String?): AsrGateMode =
            when (raw?.trim()?.lowercase()) {
                "prob" -> PROB
                "logprob" -> LOGPROB
                "off" -> OFF
                else -> AUTO
            }
    }
}

data class AsrGatePolicy(
    val enabled: Boolean = true,
    val mode: AsrGateMode = AsrGateMode.AUTO,
    val minProb: Float = AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB,
) {
    companion object {
        val DEFAULT = AsrGatePolicy()
    }
}
