package cz.handy.feature.nlu

/** Bezpečný fallback, když MediaPipe LLM nelze načíst (chybí `.task` nebo native knihovna). */
object NoMatchUtteranceParser : UtteranceNluParser {
    override suspend fun parse(utterance: String): NluResult = NluResult.NoMatch
}
