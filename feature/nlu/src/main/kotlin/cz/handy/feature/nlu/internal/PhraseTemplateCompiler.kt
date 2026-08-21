package cz.handy.feature.nlu.internal

import java.util.Locale
import java.util.regex.Pattern

private val CS = Locale.forLanguageTag("cs-CZ")

/**
 * Šablona typu `zavolej {contact}` → regex s grupami; poslední slot je greedy, prostřední lazy.
 * Bez `{…}` vznikne přesná fráze (např. `"zapni baterku"`).
 */
private data class SlotSpan(
    val name: String,
    val start: Int,
    val endExclusive: Int,
)

internal object PhraseTemplateCompiler {
    /**
     * `{slot}` placeholders are parsed without regex — Android ICU requires escaping both
     * `{` and `}` in patterns, so `Regex("""\{([^}]+)}""")` throws [PatternSyntaxException] at class init.
     */
    private fun findSlotSpans(normalized: String): List<SlotSpan> {
        val spans = mutableListOf<SlotSpan>()
        var index = 0
        while (index < normalized.length) {
            if (normalized[index] != '{') {
                index++
                continue
            }
            val close = normalized.indexOf('}', startIndex = index + 1)
            require(close > index) { "Neuzavřený slot v šabloně: $normalized" }
            val name = normalized.substring(index + 1, close).trim().lowercase(CS)
            require(name.isNotBlank()) { "Název slotu nesmí být prázdný: $normalized" }
            spans.add(SlotSpan(name = name, start = index, endExclusive = close + 1))
            index = close + 1
        }
        return spans
    }

    fun compile(phraseTemplate: String): PhraseMatcherSpec {
        val normalized = phraseTemplate.trim().lowercase(CS).replace(Regex("\\s+"), " ")
        require(normalized.isNotBlank()) { "Šablona fráze nesmí být prázdná." }

        val spans = findSlotSpans(normalized)
        if (spans.isEmpty()) {
            val pat = "^" + Pattern.quote(normalized) + "$"
            return PhraseMatcherSpec(
                regex = Regex(pat, RegexOption.IGNORE_CASE),
                orderedSlotNames = emptyList(),
            )
        }

        val orderedSlotNames = spans.map { it.name }
        check(orderedSlotNames.toSet().size == orderedSlotNames.size) {
            "Šablona nesmí opakovat název slotu: $phraseTemplate"
        }

        val regexPat =
            buildString {
                append('^')
                var cursor = 0
                spans.forEachIndexed { index, span ->
                    val literal = normalized.substring(cursor, span.start)
                    append(Pattern.quote(literal))

                    val isLastCapture = index == spans.lastIndex
                    append(if (isLastCapture) "(.+)" else "(.+?)")
                    cursor = span.endExclusive
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
