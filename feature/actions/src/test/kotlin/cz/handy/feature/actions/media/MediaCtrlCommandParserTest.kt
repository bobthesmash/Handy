package cz.handy.feature.actions.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MediaCtrlCommandParserTest {
    @Test
    fun mapsNextAndPrevious() {
        assertEquals(MediaTransportCommand.Next, MediaCtrlCommandParser.parse("další skladba"))
        assertEquals(MediaTransportCommand.Next, MediaCtrlCommandParser.parse("přeskakuj"))
        assertEquals(MediaTransportCommand.Previous, MediaCtrlCommandParser.parse("předchozí skladba"))
        assertEquals(MediaTransportCommand.Previous, MediaCtrlCommandParser.parse("zpět"))
    }

    @Test
    fun mapsPauseAndPlay() {
        assertEquals(MediaTransportCommand.Pause, MediaCtrlCommandParser.parse("pauza"))
        assertEquals(MediaTransportCommand.Pause, MediaCtrlCommandParser.parse("stop hudbu"))
        assertEquals(MediaTransportCommand.Play, MediaCtrlCommandParser.parse("pokračuj"))
        assertEquals(MediaTransportCommand.Play, MediaCtrlCommandParser.parse("resume"))
    }

    @Test
    fun unknownReturnsNull() {
        assertNull(MediaCtrlCommandParser.parse("objednej pizzu"))
    }
}
