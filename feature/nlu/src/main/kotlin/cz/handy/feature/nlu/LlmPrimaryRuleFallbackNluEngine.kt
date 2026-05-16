package cz.handy.feature.nlu

/**
 * Nejprve primární parser ([UtteranceNluParser]) — typicky [StructuredJsonUtteranceLlmParser] (JSON v textu / ASR)
 * nebo budoucí MediaPipe výstup; při [NluResult.NoMatch] stejný vstup parsuje rule engine ([F1-T08]).
 */
class LlmPrimaryRuleFallbackNluEngine(
    private val llm: UtteranceNluParser,
    private val rules: UtteranceNluParser,
) : UtteranceNluParser {
    override fun parse(utterance: String): NluResult =
        when (val first = llm.parse(utterance)) {
            is NluResult.Matched -> first
            NluResult.NoMatch -> rules.parse(utterance)
        }
}
