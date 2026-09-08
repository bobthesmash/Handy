package cz.handy.core.common.audio

import kotlin.math.exp

/**
 * Práh pro **Sherpa ysProbs** (pravděpodobnosti tokenů z [com.k2fsa.sherpa.onnx.OnlineRecognizerResult]) —
 * pod prahem žádáme opakování ([F2-T12]).
 *
 * On-device Sherpa `ysProbs` are often **log-probabilities** (negative). Comparing them raw
 * against [DEFAULT_MIN_TOKEN_PROB] silently drops every utterance. Default [AsrGateMode.AUTO]
 * maps values outside `[0, 1]` through `exp` and compares in probability space.
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

    fun asProbability(
        raw: Float,
        mode: AsrGateMode = AsrGateMode.AUTO,
    ): Float =
        when (mode) {
            AsrGateMode.PROB -> raw
            AsrGateMode.LOGPROB -> exp(raw)
            AsrGateMode.AUTO -> if (raw in 0f..1f) raw else exp(raw)
            AsrGateMode.OFF -> raw
        }

    /**
     * @param minTokenProb nejnižší ysProb z posledního výsledku, nebo `null` pokud engine nevrátil pravděpodobnosti.
     */
    fun shouldAskRepeat(
        text: String,
        minTokenProb: Float?,
        policy: AsrGatePolicy = AsrGatePolicy.DEFAULT,
    ): Boolean {
        if (text.isBlank()) return false
        if (minTokenProb == null) return false
        if (!policy.enabled || policy.mode == AsrGateMode.OFF) return false
        val p = asProbability(minTokenProb, policy.mode)
        return p < policy.minProb
    }
}
