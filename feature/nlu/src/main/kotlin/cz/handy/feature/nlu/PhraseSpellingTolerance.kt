package cz.handy.feature.nlu

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Single spelling-tolerance rule for catalog **command** phrases (~20% Levenshtein).
 * Space is ignored as a secondary comparison so ASR splits like `flesh light` still
 * match `flashlight`. Contact/place **names** are not rewritten by this tool —
 * callers must pass only command literals, never slot values.
 */
object PhraseSpellingTolerance {
    const val RELATIVE_TOLERANCE = 0.20
    const val MIN_LENGTH_FOR_EDITS = 4

    fun within(
        left: String,
        right: String,
    ): Boolean {
        val a = left.trim()
        val b = right.trim()
        if (a == b) return true
        if (a.isEmpty() || b.isEmpty()) return false
        if (editsWithinTolerance(a, b)) return true
        val compactA = a.replace(" ", "")
        val compactB = b.replace(" ", "")
        if (compactA == compactB) return true
        return editsWithinTolerance(compactA, compactB)
    }

    internal fun editsWithinTolerance(
        a: String,
        b: String,
    ): Boolean {
        val n = max(a.length, b.length)
        if (n < MIN_LENGTH_FOR_EDITS) return a == b
        val dist = levenshtein(a, b)
        val allowed = max(1, ceil(RELATIVE_TOLERANCE * n).toInt())
        return dist <= allowed
    }

    internal fun levenshtein(
        s1: String,
        s2: String,
    ): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                dp[j] =
                    if (s1[i - 1] == s2[j - 1]) {
                        prev
                    } else {
                        min(prev, min(dp[j], dp[j - 1])) + 1
                    }
                prev = temp
            }
        }
        return dp[s2.length]
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
