package cz.handy.feature.wakeword

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PorcupinePcmFrameAccumulatorTest {
    @Test
    fun concatPreservesEmptySides() {
        val a = shortArrayOf(1, 2)
        assertSame(a, concatShortArrays(a, shortArrayOf()))
        val b = shortArrayOf(3)
        assertSame(b, concatShortArrays(shortArrayOf(), b))
    }

    @Test
    fun accumulatorPollsAcrossChunkBoundary() {
        val acc = PorcupinePcmFrameAccumulator(3)
        val out = ShortArray(3)
        acc.append(shortArrayOf(1, 2))
        assertFalse(acc.pollFrame(out))
        acc.append(shortArrayOf(3, 4, 5, 6))
        assertTrue(acc.pollFrame(out))
        assertTrue(out.contentEquals(shortArrayOf(1, 2, 3)))
        assertTrue(acc.pollFrame(out))
        assertTrue(out.contentEquals(shortArrayOf(4, 5, 6)))
        assertFalse(acc.pollFrame(out))
    }

    @Test
    fun clearDiscardsQueuedSamples() {
        val acc = PorcupinePcmFrameAccumulator(4)
        val out = ShortArray(4)
        acc.append(shortArrayOf(1, 2))
        acc.clear()
        acc.append(shortArrayOf(3, 4, 5, 6))
        assertTrue(acc.pollFrame(out))
        assertTrue(out.contentEquals(shortArrayOf(3, 4, 5, 6)))
    }
}
