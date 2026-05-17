package cz.handy.feature.nlu

/**
 * Nejprve zkusí strukturovaný LLM výstup ([F5-T01]), při [NluResult.NoMatch] použije rule engine ([F1-T08]).
 */
class LlmPrimaryRuleFallbackNluEngine(
    private val llm: UtteranceNluParser,
    private val rules: UtteranceNluParser,
) : UtteranceNluParser {
    override suspend fun parse(utterance: String): NluResult =
        when (val first = llm.parse(utterance)) {
            is NluResult.Matched -> first
            NluResult.NoMatch -> rules.parse(utterance)
        }
}
