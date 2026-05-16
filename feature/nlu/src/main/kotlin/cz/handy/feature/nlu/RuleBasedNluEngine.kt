package cz.handy.feature.nlu

import cz.handy.feature.nlu.internal.IntentDefinition
import cz.handy.feature.nlu.internal.PhraseMatcherSpec

/**
 * První shora vyhovující šabloně v rámci katalogu výhra (determinismus pro testovatelnost).
 */
class RuleBasedNluEngine(
    private val catalog: IntentCatalog,
) {
    fun parse(utterance: String): NluResult {
        val n = IntentCatalog.normalizeUtterance(utterance)
        if (n.isBlank()) return NluResult.NoMatch
        return matchCatalog(catalog.intents, n)
    }

    private fun matchCatalog(
        defs: List<IntentDefinition>,
        normalized: String,
    ): NluResult {
        for (def in defs) {
            matchFirstInDefinition(def, normalized)?.let {
                return NluResult.Matched(it)
            }
        }
        return NluResult.NoMatch
    }

    private fun matchFirstInDefinition(
        def: IntentDefinition,
        normalized: String,
    ): ParsedIntent? {
        for (m in def.matchers) {
            tryParseWithMatcher(def, m, normalized)?.let { return it }
        }
        return null
    }

    private fun tryParseWithMatcher(
        def: IntentDefinition,
        m: PhraseMatcherSpec,
        normalized: String,
    ): ParsedIntent? {
        val mr = m.regex.matchEntire(normalized)
        val groups = mr?.groupValues?.drop(1)
        if (mr == null || groups == null || groups.size != m.orderedSlotNames.size) {
            return null
        }
        val slots =
            m.orderedSlotNames
                .zip(groups) { name, raw -> name to raw.trim() }
                .toMap()
        return when {
            def.slotOk(slots) ->
                ParsedIntent(
                    intentId = def.id,
                    slots = slots,
                    requiresConfirm = def.requiresConfirm,
                )
            else -> null
        }
    }
}
