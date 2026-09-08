package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals

class FuzzyWuzzyTest {
    @Test
    fun ratio_exact_and_empty() {
        assertEquals(100, FuzzyWuzzy.ratio("pause", "pause"))
        assertEquals(0, FuzzyWuzzy.ratio("pause", ""))
        assertEquals(0, FuzzyWuzzy.ratio("", "pause"))
        assertEquals(100, FuzzyWuzzy.ratio("", ""))
    }

    @Test
    fun levenshtein_empty_and_substitution() {
        assertEquals(0, FuzzyWuzzy.levenshtein("call", "call"))
        assertEquals(4, FuzzyWuzzy.levenshtein("", "call"))
        assertEquals(4, FuzzyWuzzy.levenshtein("call", ""))
        assertEquals(1, FuzzyWuzzy.levenshtein("caal", "call"))
        assertEquals(2, FuzzyWuzzy.levenshtein("butter", "battery"))
    }

    @Test
    fun token_sort_ratio_matches_sorted_ratio() {
        assertEquals(
            FuzzyWuzzy.ratio("battery is my what", "battery is my what"),
            FuzzyWuzzy.tokenSortRatio("what is my battery", "battery my what is"),
        )
    }
}
