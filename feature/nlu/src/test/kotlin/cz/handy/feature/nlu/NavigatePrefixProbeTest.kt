package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NavigatePrefixProbeTest {
    @Test
    fun exact_navigate_to_prefix_without_place() {
        val hit = NavigatePrefixProbe.match("navigate to")
        assertNotNull(hit)
        assertEquals("navigate to", hit.prefix)
        assertEquals(2, hit.wordCount)
        assertFalse(hit.placeAlreadyStarted)
    }

    @Test
    fun navigate_to_with_place_marks_tail_started() {
        val hit = NavigatePrefixProbe.match("navigate to brno")
        assertNotNull(hit)
        assertTrue(hit.placeAlreadyStarted)
        assertEquals(2, hit.wordCount)
    }

    @Test
    fun directions_to_is_a_navigate_prefix() {
        val hit = NavigatePrefixProbe.match("directions to")
        assertNotNull(hit)
        assertEquals("directions to", hit.prefix)
    }

    @Test
    fun battery_is_not_a_navigate_prefix() {
        assertNull(NavigatePrefixProbe.match("battery"))
        assertNull(NavigatePrefixProbe.match("call jane"))
    }

    @Test
    fun prefixes_come_from_en_minimal_catalog() {
        val prefixes = NavigatePrefixProbe.prefixes()
        assertTrue("navigate to" in prefixes)
        assertTrue("directions to" in prefixes)
    }
}
