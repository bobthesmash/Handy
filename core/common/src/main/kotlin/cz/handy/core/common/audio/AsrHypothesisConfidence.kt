package cz.handy.core.common.audio

import kotlin.math.exp

/**
 * Práh pro **Sherpa `ysProbs`**. On-device values are often **log-probabilities** (negative);
 * the default gate converts those with `exp` and compares in probability space ([F2-T12]).
 */
object AsrHypothesisConfidence {
    /** Pod tímto minimem považujeme hypotézu za nedůvěryhodnou (greedy CTC / zipformer). */
    const val DEFAULT_MIN_TOKEN_PROB = 0.28f

    fun minYsProbability(ysProbs: FloatArray?): Float? {
        if (ysProbs == null || ysProbs.isEmpty()) return null
        var m = ysProbs[0]
        for (i in 1 until ysProbs.size) {
            if (ysProbs[i] < m) m = ysProbs[i]
        }
        return m
    }

    /**
     * Maps a Sherpa token score into probability space according to [mode].
     */
    fun asProbability(
        rawScore: Float,
        mode: AsrGateMode = AsrGateMode.AUTO,
    ): Float =
        when (mode) {
            AsrGateMode.OFF, AsrGateMode.PROB -> rawScore
            AsrGateMode.LOGPROB -> exp(rawScore)
            AsrGateMode.AUTO ->
                if (rawScore in PROBABILITY_INCLUSIVE_RANGE) {
                    rawScore
                } else {
                    exp(rawScore)
                }
        }

    /**
     * @param minTokenProb nejnižší ysProb z posledního výsledku, nebo `null` pokud engine nevrátil pravděpodobnosti.
     */
    fun shouldAskRepeat(
        text: String,
        minTokenProb: Float?,
        minProbThreshold: Float = DEFAULT_MIN_TOKEN_PROB,
    ): Boolean =
        shouldAskRepeat(
            text,
            minTokenProb,
            AsrGatePolicy(minProb = minProbThreshold),
        )

    fun shouldAskRepeat(
        text: String,
        minTokenProb: Float?,
        policy: AsrGatePolicy,
    ): Boolean {
        if (text.isBlank() || minTokenProb == null) return false
        if (!policy.enabled || policy.mode == AsrGateMode.OFF) return false
        val probability = asProbability(minTokenProb, policy.mode)
        return !probability.isFinite() || probability < policy.minProb
    }

    private val PROBABILITY_INCLUSIVE_RANGE = 0f..1f
}
