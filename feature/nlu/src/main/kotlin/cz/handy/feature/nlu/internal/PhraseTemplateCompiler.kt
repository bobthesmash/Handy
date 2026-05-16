package cz.handy.feature.nlu.internal

import java.util.Locale
import java.util.regex.Pattern

private val CS = Locale.forLanguageTag("cs-CZ")

/**
 * Šablona typu `zavolej {contact}` → regex s grupami; poslední slot je greedy, prostřední lazy.
 * Bez `{…}` vznikne přesná fráze (např. `"zapni baterku"`).
 */
internal object PhraseTemplateCompiler {
    private val bracePlaceholder = Regex("""\{([^}]+)}""")

    fun compile(phraseTemplate: String): PhraseMatcherSpec {
        val normalized = phraseTemplate.trim().lowercase(CS).replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank()) { "Šablona fráze nesmí být prázdná." }

        val occurrences = bracePlaceholder.findAll(normalized).toList()
        if (occurrences.isEmpty()) {
            val pat = "^" + Pattern.quote(normalized) + "$"
            return PhraseMatcherSpec(
                regex = Regex(pat, RegexOption.IGNORE_CASE),
                orderedSlotNames = emptyList(),
            )
        }

        val orderedSlotNames = occurrences.map { it.groupValues[1].trim().lowercase(CS) }
        check(orderedSlotNames.toSet().size == orderedSlotNames.size) {
            "Šablona nesmí opakovat název slotu: $phraseTemplate"
        }

        val regexPat =
            buildString {
                append('^')
                var cursor = 0
                occurrences.forEachIndexed { index, match ->
                    val literal = normalized.substring(cursor, match.range.first)
                    append(Pattern.quote(literal))

                    val isLastCapture = index == occurrences.lastIndex
                    append(if (isLastCapture) "(.+)" else "(.+?)")
                    cursor = match.range.last + 1
                }
                append(Pattern.quote(normalized.substring(cursor)))
                append('$')
            }

        return PhraseMatcherSpec(
            regex = Regex(regexPat, RegexOption.IGNORE_CASE),
            orderedSlotNames = orderedSlotNames,
        )
    }
}
