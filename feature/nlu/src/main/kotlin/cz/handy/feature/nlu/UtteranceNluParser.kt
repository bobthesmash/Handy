package cz.handy.feature.nlu

/** Společný kontrakt řetězce NLU ([F5-T01] — LLM + rule fallback). */
fun interface UtteranceNluParser {
    suspend fun parse(utterance: String): NluResult
}
