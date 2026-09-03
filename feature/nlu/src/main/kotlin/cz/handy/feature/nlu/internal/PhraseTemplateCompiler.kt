package cz.handy.feature.nlu.internal

import java.util.Locale
import java.util.regex.Pattern

private val CS = Locale.forLanguageTag("cs-CZ")

/**
 * Šablona typu `zavolej {contact}` → regex s grupami; poslední slot je greedy, prostřední lazy.
 * Bez `{…}` vznikne přesná fráze (např. `"zapni baterku"`).
 */
internal object PhraseTemplateCompiler {
    private val bracePlaceholder = Regex("""\{([^}]+)\}""")

    fun compile(phraseTemplate: String): PhraseMatcherSpec {
        val normalized = phraseTemplate.trim().lowercase(CS).replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank()) { "Šablona fráze nesmí být prázdná." }

        val occurrences = bracePlaceholder.findAll(normalized).toList()
        if (occurrences.isEmpty()) {
            val pat = "^" + Pattern.quote(normalized) + "$"
            return PhraseMatcherSpec(
                regex = Regex(pat, RegexOption.IGNORE_CASE),
                orderedSlotNames = emptyList(),
                staticSlots = emptyMap(),
            )
        }

        val staticSlots = LinkedHashMap<String, String>()
        val orderedSlotNames = mutableListOf<String>()
        val variableOccurrences = occurrences.filter { !it.groupValues[1].contains("=") }

        var varIdx = 0
        val regexPat =
            buildString {
                append('^')
                var cursor = 0
                occurrences.forEach { match ->
                    val literal = normalized.substring(cursor, match.range.first)
                    append(Pattern.quote(literal))
                    cursor = match.range.last + 1

                    val inner = match.groupValues[1].trim().lowercase(CS)
                    if (inner.contains("=")) {
                        val parts = inner.split("=", limit = 2)
                        val slotName = parts[0].trim().lowercase(CS)
                        val slotValue = parts[1].trim().lowercase(CS)
                        require(slotName.isNotBlank()) { "Název slotu nesmí být prázdný: $phraseTemplate" }
                        staticSlots[slotName] = slotValue
                    } else {
                        val isLastCapture = varIdx == variableOccurrences.lastIndex
                        append(if (isLastCapture) "(.+)" else "(.+?)")
                        orderedSlotNames.add(inner)
                        varIdx++
                    }
                }
                append(Pattern.quote(normalized.substring(cursor)))
                append('$')
            }

        check((orderedSlotNames + staticSlots.keys).toSet().size == orderedSlotNames.size + staticSlots.size) {
            "Šablona nesmí opakovat název slotu: $phraseTemplate"
        }

        return PhraseMatcherSpec(
            regex = Regex(regexPat, RegexOption.IGNORE_CASE),
            orderedSlotNames = orderedSlotNames,
            staticSlots = staticSlots,
        )
    }
}
