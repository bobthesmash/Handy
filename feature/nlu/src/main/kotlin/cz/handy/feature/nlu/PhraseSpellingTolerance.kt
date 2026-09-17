package cz.handy.feature.nlu

import java.util.Locale
import kotlin.math.max

/**
 * Rhasspy / FuzzyWuzzy command-phrase matcher.
 *
 * Scores with [FuzzyWuzzy.ratio] and [FuzzyWuzzy.tokenSortRatio] at
 * [MIN_CONFIDENCE] = 80 (~20% spelling). Compound ASR splits (`flesh light` /
 * `flashlight`) are scored again with spaces removed using the same ratio.
 *
 * Contact/place **names** are never passed through this tool.
 */
object PhraseSpellingTolerance {
    const val MIN_CONFIDENCE = 80
    const val MIN_LENGTH_FOR_FUZZY = 4

    fun within(
        left: String,
        right: String,
    ): Boolean = score(left, right) >= MIN_CONFIDENCE

    fun score(
        left: String,
        right: String,
    ): Int {
        val a = left.trim()
        val b = right.trim()
        if (a == b) return 100
        if (a.isEmpty() || b.isEmpty()) return 0
        if (max(a.length, b.length) < MIN_LENGTH_FOR_FUZZY) {
            return 0
        }
        val spaced =
            max(
                FuzzyWuzzy.ratio(a, b),
                FuzzyWuzzy.tokenSortRatio(a, b),
            )
        val compactA = a.replace(" ", "")
        val compactB = b.replace(" ", "")
        if (compactA == a && compactB == b) return spaced
        return max(spaced, FuzzyWuzzy.ratio(compactA, compactB))
    }
}

/**
 * Light ASR text cleanup in front of rule NLU (not a synonym table).
 */
object AsrUtteranceNormalizer {
    private val CS = Locale.forLanguageTag("cs-CZ")
    private val FILLER_PREFIX = Regex("^(oh|hey|um|uh|ah)\\s+")
    private val EXTRA_SPACE = Regex("\\s+")
    private val CONTRACTIONS =
        listOf(
            Regex("\\bwhat's\\b") to "what is",
            Regex("\\bhow's\\b") to "how is",
            Regex("\\bit's\\b") to "it is",
            Regex("\\bthat's\\b") to "that is",
            Regex("\\bwhats\\b") to "what is",
            Regex("\\bhows\\b") to "how is",
        )

    fun normalize(raw: String): String = normalize(raw, stripLeadingFillers = true)

    fun normalizeTemplate(raw: String): String = normalize(raw, stripLeadingFillers = false)

    private fun normalize(
        raw: String,
        stripLeadingFillers: Boolean,
    ): String {
        var s =
            raw
                .trim()
                .lowercase(CS)
                .replace('\u2019', '\'')
                .replace(EXTRA_SPACE, " ")
        if (stripLeadingFillers) {
            while (FILLER_PREFIX.containsMatchIn(s)) {
                s = s.replaceFirst(FILLER_PREFIX, "")
            }
        }
        for ((pat, repl) in CONTRACTIONS) {
            s = pat.replace(s, repl)
        }
        return s.replace(EXTRA_SPACE, " ").trim()
    }
}
