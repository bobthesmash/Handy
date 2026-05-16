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
}
