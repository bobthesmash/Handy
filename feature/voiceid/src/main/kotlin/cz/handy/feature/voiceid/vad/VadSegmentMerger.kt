package cz.handy.feature.voiceid.vad

/**
 * Hystereze + minimální délka řeči a koncové ticho — zřetězení pravděpodobností po rámcích ([F1‑T05]).
 *
 * Rámce musí být v čase seřazené a **nepřekrývat se**; [frameHopSamples] určuje i krok ticha v součtech.
 */
class VadSegmentMerger(
    private val thresholdOn: Float,
    private val thresholdOff: Float,
    private val minSpeechSamples: Int,
    private val minTrailingSilenceSamples: Int,
    private val frameHopSamples: Int,
) {
    init {
        require(thresholdOff <= thresholdOn) {
            "Need thresholdOff ≤ thresholdOn (got off=$thresholdOff on=$thresholdOn)."
        }
        require(minSpeechSamples > 0)
        require(minTrailingSilenceSamples > 0)
        require(frameHopSamples > 0)
    }

    fun merge(frames: List<VadFrame>): List<VadSegment> {
        if (frames.isEmpty()) return emptyList()
        var inSegment = false
        var segmentStart = 0
        var lastSpeechEndExclusive = 0
        var trailingSilenceSamples = 0
        val out = ArrayList<VadSegment>()

        for (frame in frames) {
            val hop = (frame.endExclusive - frame.startInclusive).coerceAtLeast(1)
            if (!inSegment) {
                if (frame.speechProb >= thresholdOn) {
                    inSegment = true
                    segmentStart = frame.startInclusive
                    lastSpeechEndExclusive = frame.endExclusive
                    trailingSilenceSamples = 0
                }
            } else {
                if (frame.speechProb < thresholdOff) {
                    trailingSilenceSamples += hop
                    if (trailingSilenceSamples >= minTrailingSilenceSamples) {
                        maybeEmit(segmentStart, lastSpeechEndExclusive, out)
                        inSegment = false
                        trailingSilenceSamples = 0
                    }
                } else {
                    trailingSilenceSamples = 0
                    lastSpeechEndExclusive = frame.endExclusive
                }
            }
        }
        if (inSegment) {
            maybeEmit(segmentStart, lastSpeechEndExclusive, out)
        }
        return out
    }

    private fun maybeEmit(
        startInclusive: Int,
        endExclusive: Int,
        out: MutableList<VadSegment>,
    ) {
        if (endExclusive - startInclusive >= minSpeechSamples) {
            out.add(VadSegment(startInclusive, endExclusive))
        }
    }
}
