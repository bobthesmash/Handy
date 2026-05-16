package cz.handy.core.audio

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class MonoPcmRingBufferTest {
    @Test
    fun contiguousFillMatchesInput() {
        val buf = MonoPcmRingBuffer(128)
        val input = ShortArray(100) { it.toShort() }
        buf.write(input, 0, input.size)
        assertContentEquals(input, buf.copyOldestFirst())
    }

    @Test
    fun overwritesOldestSamplesWhenFull() {
        val buf = MonoPcmRingBuffer(4)
        buf.write(shortArrayOf(1, 2, 3, 4))
        assertContentEquals(shortArrayOf(1, 2, 3, 4), buf.copyOldestFirst())

        buf.write(shortArrayOf(100))
        assertContentEquals(shortArrayOf(2, 3, 4, 100), buf.copyOldestFirst())

        buf.write(shortArrayOf(200, 201))
        assertContentEquals(shortArrayOf(4, 100, 200, 201), buf.copyOldestFirst())
    }

    @Test
    fun preRollCapacityMatchesThreeSecondsAtSixteenKh() {
        assertEquals(48_000, MicCaptureConfig.ringCapacitySamples)
    }

    @Test
    fun consumeIncrementalMatchesContiguousWrite() {
        val buf = MonoPcmRingBuffer(128)
        var marker = buf.totalSamplesWritten()
        buf.write(shortArrayOf(1, 2, 3))
        val (a, m1) = buf.consumeMono16SinceTotalWritten(marker)
        marker = m1
        assertContentEquals(shortArrayOf(1, 2, 3), a)
        buf.write(shortArrayOf(4, 5))
        val (b, _) = buf.consumeMono16SinceTotalWritten(marker)
        assertContentEquals(shortArrayOf(4, 5), b)
    }

    @Test
    fun consumeSkipsWhenReaderFallsBehindCapacity() {
        val buf = MonoPcmRingBuffer(4)
        buf.write(shortArrayOf(1, 2, 3, 4, 5, 6))
        val (chunk, tw) = buf.consumeMono16SinceTotalWritten(0L)
        assertEquals(6L, tw)
        assertEquals(4, chunk.size)
        assertContentEquals(shortArrayOf(3, 4, 5, 6), chunk)
    }
}
