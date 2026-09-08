package cz.handy.feature.nlu

import cz.handy.feature.nlu.internal.IntentDefinition
import cz.handy.feature.nlu.internal.PhraseMatcherSpec

/**
 * Fallback after exact regex miss: ~20% spelling tolerance on **command** literals.
 * Capture slots that hold names ([NAME_SLOTS]) are copied as spoken — never fuzzy-rewritten.
 */
internal object CatalogPhraseFuzzyMatcher {
    internal val NAME_SLOTS = setOf("contact", "place", "target")

    fun matchFirst(
        defs: List<IntentDefinition>,
        normalized: String,
    ): ParsedIntent? {
        for (def in defs) {
            matchFirstInDefinition(def, normalized)?.let { return it }
        }
        return null
    }

    private fun matchFirstInDefinition(
        def: IntentDefinition,
        normalized: String,
    ): ParsedIntent? {
        for (m in def.matchers) {
            tryMatch(def, m, normalized)?.let { return it }
        }
        return null
    }

    private fun tryMatch(
        def: IntentDefinition,
        m: PhraseMatcherSpec,
        normalized: String,
    ): ParsedIntent? {
        val slots =
            when {
                m.orderedSlotNames.isEmpty() -> matchStatic(m, normalized) ?: return null
                m.orderedSlotNames.size == 1 -> matchTrailingSlot(m, normalized) ?: return null
                else -> return null
            }
        if (!def.slotOk(slots)) return null
        return ParsedIntent(
            intentId = def.id,
            slots = slots,
            requiresConfirm = def.requiresConfirm,
        )
    }

    private fun matchStatic(
        m: PhraseMatcherSpec,
        normalized: String,
    ): Map<String, String>? {
        if (m.literalPhrase.isBlank()) return null
        if (!PhraseSpellingTolerance.within(normalized, m.literalPhrase)) return null
        return m.staticSlots
    }

    /**
     * `call {contact}` / `navigate to {place}`: fuzzy the command prefix only;
     * the remainder is the name and stays exactly as recognized.
     */
    private fun matchTrailingSlot(
        m: PhraseMatcherSpec,
        normalized: String,
    ): Map<String, String>? {
        val slotName = m.orderedSlotNames.single()
        val prefix = m.leadingLiteral.trim()
        if (prefix.isEmpty()) return null
        val prefixWords = prefix.split(' ').filter { it.isNotEmpty() }
        val uttWords = normalized.split(' ').filter { it.isNotEmpty() }
        if (uttWords.size <= prefixWords.size) return null
        val uttPrefix = uttWords.take(prefixWords.size).joinToString(" ")
        if (!PhraseSpellingTolerance.within(uttPrefix, prefix)) return null
        val slotValue = uttWords.drop(prefixWords.size).joinToString(" ")
        if (slotValue.isBlank()) return null
        return m.staticSlots + (slotName to slotValue)
    }
}
