package cz.handy.feature.nlu

/**
 * Nejlepší shoda z pravidlového NLU ([F1‑T08]); sloty vznikají ze šablon `{jméno}`.
 */
data class ParsedIntent(
    val intentId: String,
    val slots: Map<String, String>,
    /** Napojení na `DialogManager.onNluComplete` ([F1‑T07] / [F1‑T16]). */
    val requiresConfirm: Boolean,
)

sealed interface NluResult {
    data class Matched(
        val intent: ParsedIntent,
    ) : NluResult

    data object NoMatch : NluResult
}
