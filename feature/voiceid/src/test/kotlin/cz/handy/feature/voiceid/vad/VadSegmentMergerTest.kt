package cz.handy.feature.voiceid.vad

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VadSegmentMergerTest {
    private val merger =
        VadSegmentMerger(
            thresholdOn = 0.55f,
            thresholdOff = 0.38f,
            minSpeechSamples = 5 * STEP,
            minTrailingSilenceSamples = 3 * STEP,
            frameHopSamples = STEP,
        )

    @Test
    fun rejectsAllQuiet() {
        val frames =
            framesFromProbs(List(16) { 0.1f })
        assertEquals(emptyList(), merger.merge(frames))
    }

    @Test
    fun oneLongSpeechBlockBecomesSegment() {
        val probs = List(8) { 0.1f } + List(16) { 0.9f } + List(12) { 0.1f }
        val frames = framesFromProbs(probs)
        val segs = merger.merge(frames)
        assertEquals(1, segs.size)
        assertEquals(8 * STEP, segs[0].startInclusive)
        assertEquals((8 + 16) * STEP, segs[0].endExclusive)
    }

    @Test
    fun shortBlipBelowMinSpeechIsDropped() {
        val probs = List(4) { 0.1f } + List(3) { 0.95f } + List(8) { 0.1f }
        val frames = framesFromProbs(probs)
        assertTrue(merger.merge(frames).isEmpty())
    }

    @Test
    fun twoSeparatedUtterances() {
        val probs =
            List(4) { 0.05f } +
                List(10) { 0.95f } +
                List(10) { 0.08f } +
                List(10) { 0.95f } +
                List(8) { 0.05f }
        val segs = merger.merge(framesFromProbs(probs))
        assertEquals(2, segs.size)
        assertEquals(4 * STEP, segs[0].startInclusive)
        assertEquals((4 + 10) * STEP, segs[0].endExclusive)
        assertEquals((4 + 10 + 10) * STEP, segs[1].startInclusive)
    }

    private fun framesFromProbs(probs: List<Float>): List<VadFrame> =
        probs.mapIndexed { i, p ->
            val start = i * STEP
            VadFrame(startInclusive = start, endExclusive = start + STEP, speechProb = p)
        }

    private companion object {
        const val STEP = 512
    }
}
