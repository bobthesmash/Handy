package cz.handy.feature.actions.timer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CzechDurationParserTest {
    @Test
    fun blank_returnsNull() {
        assertNull(CzechDurationParser.parseToSeconds("   "))
    }

    @Test
    fun numericMinutesHoursSeconds() {
        assertEquals(600, CzechDurationParser.parseToSeconds("10 minut"))
        assertEquals(7200, CzechDurationParser.parseToSeconds("2 hodin"))
        assertEquals(45, CzechDurationParser.parseToSeconds("45 sekund"))
        assertEquals(300, CzechDurationParser.parseToSeconds("5 min"))
        assertEquals(3600, CzechDurationParser.parseToSeconds("1 hod"))
    }

    @Test
    fun wordMinutes_andFixedPhrases() {
        assertEquals(300, CzechDurationParser.parseToSeconds("pět minut"))
        assertEquals(60, CzechDurationParser.parseToSeconds("jednu minutu"))
        assertEquals(1800, CzechDurationParser.parseToSeconds("půl hodiny"))
        assertEquals(900, CzechDurationParser.parseToSeconds("čtvrt hodiny"))
    }

    @Test
    fun capsAtMaxSeconds() {
        assertEquals(CzechDurationParser.MAX_SECONDS, CzechDurationParser.parseToSeconds("20 hodin"))
    }
}
