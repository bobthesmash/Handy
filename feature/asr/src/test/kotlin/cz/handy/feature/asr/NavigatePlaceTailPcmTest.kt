package cz.handy.feature.asr

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatePlaceTailPcmTest {
    @Test
    fun slices_after_recorded_prefix_end() {
        val pcm = shortArrayOf(1, 2, 3, 4, 5, 6)
        val tail = NavigatePlaceTailPcm.slice(pcm, prefixEndSample = 2, prefixWordCount = 2, englishWordCount = 4)
        assertContentEquals(shortArrayOf(3, 4, 5, 6), tail)
        assertTrue(NavigatePlaceTailPcm.slicedTailOnly(pcm, 2))
    }

    @Test
    fun estimates_cut_from_word_counts_when_prefix_end_missing() {
        val pcm = ShortArray(10) { it.toShort() }
        val tail = NavigatePlaceTailPcm.slice(pcm, prefixEndSample = null, prefixWordCount = 2, englishWordCount = 5)
        assertEquals(6, tail.size)
        assertContentEquals(pcm.copyOfRange(4, 10), tail)
        assertFalse(NavigatePlaceTailPcm.slicedTailOnly(pcm, null))
    }

    @Test
    fun empty_utterance_stays_empty() {
        val tail = NavigatePlaceTailPcm.slice(ShortArray(0), 0, 2, 3)
        assertEquals(0, tail.size)
    }
}
