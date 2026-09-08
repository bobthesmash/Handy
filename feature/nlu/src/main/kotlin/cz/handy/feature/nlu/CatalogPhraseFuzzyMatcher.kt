package cz.handy.feature.nlu

import cz.handy.feature.nlu.internal.IntentDefinition
import cz.handy.feature.nlu.internal.PhraseMatcherSpec

/**
 * Rhasspy-style fallback after exact regex miss: FuzzyWuzzy `ratio` /
 * `token_sort_ratio` on **command** phrases with
 * [PhraseSpellingTolerance.MIN_CONFIDENCE]. Capture slots that hold names
 * ([NAME_SLOTS]) are copied as spoken — never fuzzy-rewritten.
 */
internal object CatalogPhraseFuzzyMatcher {
    internal val NAME_SLOTS = setOf("contact", "place", "target")

    fun matchFirst(
        defs: List<IntentDefinition>,
        normalized: String,
    ): ParsedIntent? {
        pickBestStatic(defs, normalized)?.let { return it }
        for (def in defs) {
            matchTrailingInDefinition(def, normalized)?.let { return it }
        }
        return null
    }

    /**
     * `process.extractOne(..., scorer=fuzz.ratio, score_cutoff=80)`: highest
     * score among slot-less catalog phrases. Tie → longer phrase (so `light of`
     * prefers `light off` over `light on`).
     */
    private fun pickBestStatic(
        defs: List<IntentDefinition>,
        normalized: String,
    ): ParsedIntent? {
        var best: ParsedIntent? = null
        var bestScore = -1
        var bestLen = -1
        for (def in defs) {
            for (m in def.matchers) {
                if (m.orderedSlotNames.isNotEmpty()) continue
                val lit = m.literalPhrase
                if (lit.isBlank() || '{' in lit) continue
                val score = PhraseSpellingTolerance.score(normalized, lit)
                if (score < PhraseSpellingTolerance.MIN_CONFIDENCE) continue
                val slots = m.staticSlots
                if (!def.slotOk(slots)) continue
                val better =
                    score > bestScore ||
                        (score == bestScore && lit.length > bestLen)
                if (!better) continue
                bestScore = score
                bestLen = lit.length
                best =
                    ParsedIntent(
                        intentId = def.id,
                        slots = slots,
                        requiresConfirm = def.requiresConfirm,
                    )
            }
        }
        return best
    }

    private fun matchTrailingInDefinition(
        def: IntentDefinition,
        normalized: String,
    ): ParsedIntent? {
        for (m in def.matchers) {
            if (m.orderedSlotNames.size != 1) continue
            tryTrailing(def, m, normalized)?.let { return it }
        }
        return null
    }

    /**
     * `call {contact}` / `navigate to {place}`: FuzzyWuzzy the command prefix only;
     * the remainder is the name and stays exactly as recognized.
     */
    private fun tryTrailing(
        def: IntentDefinition,
        m: PhraseMatcherSpec,
        normalized: String,
    ): ParsedIntent? {
        val slotName = m.orderedSlotNames.single()
        val prefix = m.leadingLiteral.trim()
        val prefixWords = prefix.split(' ').filter { it.isNotEmpty() }
        val uttWords = normalized.split(' ').filter { it.isNotEmpty() }
        if (prefix.isEmpty() || uttWords.size <= prefixWords.size) return null
        val uttPrefix = uttWords.take(prefixWords.size).joinToString(" ")
        if (!PhraseSpellingTolerance.within(uttPrefix, prefix)) return null
        val slotValue = uttWords.drop(prefixWords.size).joinToString(" ")
        val slots = m.staticSlots + (slotName to slotValue)
        if (slotValue.isBlank() || !def.slotOk(slots)) return null
        return ParsedIntent(
            intentId = def.id,
            slots = slots,
            requiresConfirm = def.requiresConfirm,
        )
    }
}
