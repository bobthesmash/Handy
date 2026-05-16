package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals

class UnbundledLlmNluParserTest {
    @Test
    fun `always yields NoMatch until MediaPipe bundle is wired`() {
        assertEquals(NluResult.NoMatch, UnbundledLlmNluParser.parse(""))
        assertEquals(NluResult.NoMatch, UnbundledLlmNluParser.parse("zapni baterku"))
    }
}
