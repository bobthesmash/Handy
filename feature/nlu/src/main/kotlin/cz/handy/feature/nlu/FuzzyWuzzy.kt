package cz.handy.feature.nlu

import kotlin.math.min
import kotlin.math.round

/**
 * Tiny FuzzyWuzzy / Rhasspy scorer: [ratio] and [tokenSortRatio] only.
 *
 * Rhasspy-fuzzywuzzy calls `rapidfuzz.process.extractOne(..., scorer=fuzz.ratio)`.
 * SeatGeek FuzzyWuzzy's `token_sort_ratio` is the same ratio after sorting tokens
 * (word-order ASR). WRatio / partialRatio / tokenSetRatio are **not** used — they
 * let a long utterance latch onto a short catalog phrase (`battery status` → `battery`).
 *
 * [ratio] is `(len1 + len2 - dist) / (len1 + len2)` with **unit-cost** Levenshtein
 * (replace = 1). RapidFuzz `fuzz.ratio` uses Indel (replace = 2), which scores the
 * live S23 pair `butter`/`battery` at 77 and would miss min_confidence 80. Unit cost
 * is the same formula with ~20% spelling ≈ 80, matching Rhasspy's documented
 * Levenshtein matching rather than a new protocol.
 */
internal object FuzzyWuzzy {
    private val WHITESPACE = Regex("\\s+")

    fun ratio(
        s1: String,
        s2: String,
    ): Int {
        if (s1 == s2) return 100
        if (s1.isEmpty() || s2.isEmpty()) return 0
        val dist = levenshtein(s1, s2)
        val lensum = s1.length + s2.length
        return round(100.0 * (lensum - dist) / lensum).toInt()
    }

    fun tokenSortRatio(
        s1: String,
        s2: String,
    ): Int = ratio(sortTokens(s1), sortTokens(s2))

    internal fun levenshtein(
        s1: String,
        s2: String,
    ): Int {
        var prev = IntArray(s2.length + 1) { it }
        var curr = IntArray(s2.length + 1)
        for (i in 1..s1.length) {
            curr[0] = i
            val c1 = s1[i - 1]
            for (j in 1..s2.length) {
                val cost = if (c1 == s2[j - 1]) 0 else 1
                curr[j] =
                    min(
                        prev[j] + 1,
                        min(curr[j - 1] + 1, prev[j - 1] + cost),
                    )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[s2.length]
    }

    private fun sortTokens(s: String): String =
        s
            .split(WHITESPACE)
            .filter { it.isNotEmpty() }
            .sorted()
            .joinToString(" ")
}
