package cz.handy.feature.nlu

/**
 * Zástupný LLM NLU před integrací MediaPipe + lokálních vah ([`F5-T01`]).
 *
 * Bez modelu v assets vždy [NluResult.NoMatch] → projdou výhradně pravidla (`LlmPrimaryRuleFallbackNluEngine`).
 */
object UnbundledLlmNluParser : UtteranceNluParser {
    override fun parse(
        @Suppress("UNUSED_PARAMETER") utterance: String,
    ): NluResult = NluResult.NoMatch
}
