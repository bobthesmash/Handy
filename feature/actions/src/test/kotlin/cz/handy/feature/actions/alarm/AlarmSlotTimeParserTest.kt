package cz.handy.feature.actions.alarm

import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmSlotTimeParserTest {
    @Test
    fun fourDigit_evening() {
        assertEquals(Pair(19, 15), AlarmSlotTimeParser.parseHourMinute("1915"))
    }

    @Test
    fun colon_with_space() {
        assertEquals(Pair(6, 30), AlarmSlotTimeParser.parseHourMinute("6 : 30"))
    }

    @Test
    fun single_hour() {
        assertEquals(Pair(7, 0), AlarmSlotTimeParser.parseHourMinute("7"))
    }

    @Test
    fun clamps_overlong_minutes_in_colon_form() {
        assertEquals(Pair(12, 59), AlarmSlotTimeParser.parseHourMinute("12:99"))
    }
}
