package cz.handy.feature.nlu

/**
 * Zkusí primární parser (např. český katalog), pak sekundární (např. anglický overlay; [F5-T03]).
 */
class ChainedUtteranceParsers(
    private val primary: UtteranceNluParser,
    private val secondary: UtteranceNluParser,
) : UtteranceNluParser {
    override suspend fun parse(utterance: String): NluResult =
        when (val first = primary.parse(utterance)) {
            is NluResult.Matched -> first
            NluResult.NoMatch -> secondary.parse(utterance)
        }
}
