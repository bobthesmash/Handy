package cz.handy.feature.nlu

import cz.handy.feature.nlu.internal.IntentDefinition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LlmPrimaryRuleFallbackNluEngineTest {
    private val rules =
        RuleBasedNluEngine(HandyNluCatalogs.mvp)

    @Test
    fun `llm no match falls back to rules`() {
        val engine =
            LlmPrimaryRuleFallbackNluEngine(
                llm = UnbundledLlmNluParser,
                rules = rules,
            )

        val out =
            engine.parse(
                "zapni baterku",
            )

        val m = assertIs<NluResult.Matched>(out)
        assertEquals("TORCH", m.intent.intentId)
    }

    @Test
    fun `llm match skips rules`() {
        val llm =
            UtteranceNluParser {
                NluResult.Matched(
                    ParsedIntent(
                        intentId = "LLM_ONLY",
                        slots = emptyMap(),
                        requiresConfirm = false,
                    ),
                )
            }
        val failingRules =
            RuleBasedNluEngine(IntentCatalog(emptyList<IntentDefinition>()))
        val engine =
            LlmPrimaryRuleFallbackNluEngine(
                llm = llm,
                rules = failingRules,
            )

        val out = engine.parse("cokoli")
        assertEquals("LLM_ONLY", (out as NluResult.Matched).intent.intentId)
    }

    @Test
    fun `json utterance matched by llm layer without rules`() {
        val failingRules =
            RuleBasedNluEngine(IntentCatalog(emptyList<IntentDefinition>()))
        val engine =
            LlmPrimaryRuleFallbackNluEngine(
                llm = StructuredJsonUtteranceLlmParser(HandyNluCatalogs.mvp),
                rules = failingRules,
            )
        val out =
            engine.parse(
                """{"intent":"TORCH","slots":{"mode":"zapni"}}""",
            )
        val m = assertIs<NluResult.Matched>(out)
        assertEquals("TORCH", m.intent.intentId)
    }

    @Test
    fun `structured json llm natural language falls back to rules like stub`() {
        val engine =
            LlmPrimaryRuleFallbackNluEngine(
                llm = StructuredJsonUtteranceLlmParser(HandyNluCatalogs.mvp),
                rules = rules,
            )
        val out = engine.parse("zapni baterku")
        val m = assertIs<NluResult.Matched>(out)
        assertEquals("TORCH", m.intent.intentId)
    }

    @Test
    fun `stub llm no match blank utterance stays NoMatch after rule fallback`() {
        val engine =
            LlmPrimaryRuleFallbackNluEngine(
                llm = UnbundledLlmNluParser,
                rules = rules,
            )
        assertEquals(NluResult.NoMatch, engine.parse("   "))
    }
}
