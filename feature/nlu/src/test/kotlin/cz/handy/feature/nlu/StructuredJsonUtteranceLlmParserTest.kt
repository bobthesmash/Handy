package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class StructuredJsonUtteranceLlmParserTest {
    private val catalog = HandyNluCatalogs.mvp
    private val parser = StructuredJsonUtteranceLlmParser(catalog)

    @Test
    fun `natural language yields NoMatch`() {
        assertEquals(NluResult.NoMatch, parser.parse("  zapni baterku  "))
    }

    @Test
    fun `leading whitespace before brace still parses json`() {
        val out =
            parser.parse(
                """  { "intent":"WHAT_TIME" }  """,
            )
        assertIs<NluResult.Matched>(out)
        assertEquals("WHAT_TIME", out.intent.intentId)
    }

    @Test
    fun `json torch matched`() {
        val out =
            parser.parse(
                """{"intent":"TORCH","slots":{"mode":"zapni"}}""",
            )
        val m = assertIs<NluResult.Matched>(out)
        assertEquals("TORCH", m.intent.intentId)
    }
}
