package cz.handy.feature.nlu.internal

/** Vnitřní reprezentace intentu zkompilovaného z DSL. */
internal data class IntentDefinition(
    val id: String,
    val requiresConfirm: Boolean,
    val matchers: List<PhraseMatcherSpec>,
    /** Lowercase název slotu → vyžaduje neprázdnou hodnotu. */
    val slotRequired: Map<String, Boolean>,
) {
    fun slotOk(slots: Map<String, String>): Boolean {
        for ((name, required) in slotRequired) {
            if (required && slots[name].isNullOrBlank()) return false
        }
        return true
    }
}

internal data class PhraseMatcherSpec(
    val regex: Regex,
    val orderedSlotNames: List<String>,
)
