package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LlmNluJsonCodecTest {
    private val catalog = HandyNluCatalogs.mvp

    @Test
    fun `empty json object is NoMatch`() {
        assertEquals(NluResult.NoMatch, LlmNluJsonCodec.parse("{}", catalog))
    }

    @Test
    fun `parses torch with slots`() {
        val raw = """{ "intent":"TORCH", "slots":{"mode":"zapni"} }"""
        val r = LlmNluJsonCodec.parse(raw, catalog)
        val m = assertIs<NluResult.Matched>(r)
        assertEquals("TORCH", m.intent.intentId)
        assertEquals(mapOf("mode" to "zapni"), m.intent.slots)
        assertEquals(false, m.intent.requiresConfirm)
    }

    @Test
    fun `unknown intent is NoMatch`() {
        assertEquals(
            NluResult.NoMatch,
            LlmNluJsonCodec.parse("""{"intent":"NOPE","slots":{}}""", catalog),
        )
    }

    @Test
    fun `invalid json is NoMatch`() {
        assertEquals(NluResult.NoMatch, LlmNluJsonCodec.parse("not json", catalog))
    }

    @Test
    fun `missing required slot is NoMatch`() {
        val raw = """{"intent":"CALL","slots":{}}"""
        assertEquals(NluResult.NoMatch, LlmNluJsonCodec.parse(raw, catalog))
    }

    @Test
    fun `what time without slots matches`() {
        val raw = """{"intent":"WHAT_TIME"}"""
        val m = assertIs<NluResult.Matched>(LlmNluJsonCodec.parse(raw, catalog))
        assertEquals("WHAT_TIME", m.intent.intentId)
        assertEquals(emptyMap(), m.intent.slots)
    }

    @Test
    fun `strips unknown slot keys`() {
        val raw =
            """
            {"intent":"TORCH","slots":{"mode":"zapni","evil":"x"}}
            """.trimIndent()
        val m = assertIs<NluResult.Matched>(LlmNluJsonCodec.parse(raw, catalog))
        assertEquals(mapOf("mode" to "zapni"), m.intent.slots)
    }

    @Test
    fun `cannot weaken catalog requiresConfirm`() {
        val raw = """{"intent":"CALL","slots":{"contact":"Jan"},"requiresConfirm":false}"""
        val m = assertIs<NluResult.Matched>(LlmNluJsonCodec.parse(raw, catalog))
        assertEquals(true, m.intent.requiresConfirm)
    }

    @Test
    fun `can strengthen requiresConfirm when catalog false`() {
        val raw = """{"intent":"TORCH","slots":{"mode":"zapni"},"requiresConfirm":true}"""
        val m = assertIs<NluResult.Matched>(LlmNluJsonCodec.parse(raw, catalog))
        assertEquals(true, m.intent.requiresConfirm)
    }

    @Test
    fun `normalizes slot keys to lowercase cs`() {
        val raw = """{"intent":"TORCH","slots":{"MODE":" vypni "}}"""
        val m = assertIs<NluResult.Matched>(LlmNluJsonCodec.parse(raw, catalog))
        assertEquals(mapOf("mode" to "vypni"), m.intent.slots)
    }
}
