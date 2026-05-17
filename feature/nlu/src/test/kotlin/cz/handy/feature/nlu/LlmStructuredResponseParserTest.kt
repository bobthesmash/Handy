package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LlmStructuredResponseParserTest {
    private val catalog = HandyNluCatalogs.mvp

    @Test
    fun parses_plain_json_torch() {
        val raw = """{"intentId":"TORCH","slots":{"mode":"zapni"},"requiresConfirm":false}"""
        val out = LlmStructuredResponseParser.parseFromRaw(raw, catalog)
        val m = assertIs<NluResult.Matched>(out)
        assertEquals("TORCH", m.intent.intentId)
        assertEquals("zapni", m.intent.slots["mode"])
    }

    @Test
    fun strips_markdown_fence() {
        val raw =
            """
            Here:
            ```json
            {"intentId":"WHAT_TIME","slots":{}}
            ```
            """.trimIndent()
        val out = LlmStructuredResponseParser.parseFromRaw(raw, catalog)
        val m = assertIs<NluResult.Matched>(out)
        assertEquals("WHAT_TIME", m.intent.intentId)
        assertEquals(false, m.intent.requiresConfirm)
    }

    @Test
    fun rejects_unknown_intent() {
        val raw = """{"intentId":"NOT_REAL","slots":{}}"""
        assertEquals(NluResult.NoMatch, LlmStructuredResponseParser.parseFromRaw(raw, catalog))
    }
}
