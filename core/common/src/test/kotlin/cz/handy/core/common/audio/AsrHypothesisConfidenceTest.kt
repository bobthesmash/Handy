package cz.handy.core.common.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsrHypothesisConfidenceTest {
    @Test
    fun minYsProbability_empty_null() {
        assertEquals(null, AsrHypothesisConfidence.minYsProbability(null))
        assertEquals(null, AsrHypothesisConfidence.minYsProbability(floatArrayOf()))
    }

    @Test
    fun minYsProbability_findsMin() {
        assertEquals(0.1f, AsrHypothesisConfidence.minYsProbability(floatArrayOf(0.9f, 0.1f, 0.5f))!!)
    }

    @Test
    fun shouldAskRepeat_onlyWhenProbBelowThreshold() {
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("", 0.1f))
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("ahoj", null))
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("ahoj", 0.9f))
        assertTrue(AsrHypothesisConfidence.shouldAskRepeat("ahoj", 0.1f))
    }
}
