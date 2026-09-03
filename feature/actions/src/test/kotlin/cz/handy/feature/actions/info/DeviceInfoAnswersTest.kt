package cz.handy.feature.actions.info

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceInfoAnswersTest {
    @Test
    fun timeSentence_respectsClock() {
        val instant = Instant.parse("2026-05-15T10:05:00Z")
        val zone = ZoneId.of("Europe/Prague")
        val clock = Clock.fixed(instant, zone)
        assertEquals("It is 12:05 PM.", DeviceInfoAnswers.timeSentence(clock))
    }

    @Test
    fun dateSentence_containsYearFromClock() {
        val instant = Instant.parse("2026-05-15T12:00:00Z")
        val zone = ZoneId.of("Europe/Prague")
        val clock = Clock.fixed(instant, zone)
        val s = DeviceInfoAnswers.dateSentence(zone, clock)
        assertTrue(s.contains("2026"), "expected year in $s")
        assertTrue(s.startsWith("It is "), s)
    }
}
