package cz.handy.core.common.audio

/**
 * Práh pro **Sherpa ysProbs** (pravděpodobnosti tokenů z [com.k2fsa.sherpa.onnx.OnlineRecognizerResult]) —
 * pod prahem žádáme opakování ([F2-T12]).
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
     * @param minTokenProb nejnižší ysProb z posledního výsledku, nebo `null` pokud engine nevrátil pravděpodobnosti.
     */
    fun shouldAskRepeat(
        text: String,
        minTokenProb: Float?,
        minProbThreshold: Float = DEFAULT_MIN_TOKEN_PROB,
    ): Boolean {
        if (text.isBlank()) return false
        if (minTokenProb == null) return false
        return minTokenProb < minProbThreshold
    }
}
