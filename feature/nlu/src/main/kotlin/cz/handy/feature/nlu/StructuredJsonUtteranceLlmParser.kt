package cz.handy.feature.nlu

/**
 * Primární „LLM“ vrstva před pravidly ([ADR-0008], [F5-T01]): pokud ASR/vstup začíná JSON objektem,
 * parsuje se přes [LlmNluJsonCodec]. Běžná mluvená věta → [NluResult.NoMatch] a řízení přejde na pravidla.
 *
 * Po přidání MediaPipe se třída rozšíří nebo nahradí parserem, který z přirozené věty vygeneruje JSON
 * interně; veřejný kontrakt katalogu zůstává.
 */
class StructuredJsonUtteranceLlmParser(
    private val catalog: IntentCatalog,
) : UtteranceNluParser {
    override fun parse(utterance: String): NluResult {
        val t = utterance.trim()
        if (!t.startsWith("{")) return NluResult.NoMatch
        return LlmNluJsonCodec.parse(t, catalog)
    }
}
