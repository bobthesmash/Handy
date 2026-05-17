package cz.handy.feature.asr

import kotlin.test.Test
import kotlin.test.assertEquals

class ParseVoskJsonTextTest {
    @Test
    fun parses_final_text() {
        assertEquals("ahoj světe", parseVoskJsonText("""{"text": "ahoj světe"}"""))
    }

    @Test
    fun parses_partial_when_text_missing() {
        assertEquals("částeč", parseVoskJsonText("""{"partial": "částeč"}"""))
    }
}
