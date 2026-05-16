package cz.handy.feature.nlu

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Parsování strukturovaného JSON z lokálního LLM ([`F5-T01`], ADR-0008) na [NluResult].
 *
 * Formát: `{"intent":"TORCH","slots":{"mode":"zapni"},"requiresConfirm":false}`
 * Pořadí klíčů libovolné; neznámé klíče ignorujeme. Sloty mimo katalog se zahodí.
 *
 * [ParsedIntent.requiresConfirm]: katalog nesmí být „zeslabený“ — pokud má intent v katalogu
 * `requiresConfirm == true`, zůstane `true` i když model vrátí `false`.
 */
object LlmNluJsonCodec {
    private val jsonFormat =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    @Serializable
    internal data class Payload(
        val intent: String,
        val slots: Map<String, String> = emptyMap(),
        val requiresConfirm: Boolean? = null,
    )

    fun parse(
        jsonText: String,
        catalog: IntentCatalog,
    ): NluResult {
        val trimmed = jsonText.trim()
        if (!trimmed.startsWith("{")) return NluResult.NoMatch
        val dto =
            runCatching { jsonFormat.decodeFromString<Payload>(trimmed) }.getOrNull()
                ?: return NluResult.NoMatch
        val intentId = dto.intent.trim()
        val baseConfirm = catalog.requiresConfirmForIntent(intentId)
        val slots = catalog.llmSlotMapOrNull(intentId, dto.slots)
        if (intentId.isEmpty() || baseConfirm == null || slots == null) {
            return NluResult.NoMatch
        }
        val requiresConfirm = baseConfirm || dto.requiresConfirm == true
        return NluResult.Matched(
            ParsedIntent(
                intentId = intentId,
                slots = slots,
                requiresConfirm = requiresConfirm,
            ),
        )
    }
}
