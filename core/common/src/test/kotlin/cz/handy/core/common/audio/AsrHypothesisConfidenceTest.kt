package cz.handy.core.common.audio

import kotlin.math.exp
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

    @Test
    fun shouldAskRepeat_acceptsSherpaLogProbAboveThreshold() {
        // Device logcat: minTokenProb=-0.602 while threshold is 0.28 in probability space.
        val logProb = -0.602f
        assertTrue(exp(logProb) > AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB)
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", logProb))
    }

    @Test
    fun shouldAskRepeat_rejectsVeryLowLogProb() {
        val logProb = -2.0f
        assertTrue(exp(logProb) < AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB)
        assertTrue(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", logProb))
    }

    @Test
    fun asProbability_auto_expWhenOutsideUnitInterval() {
        assertEquals(0.4f, AsrHypothesisConfidence.asProbability(0.4f))
        assertEquals(exp(-0.602f), AsrHypothesisConfidence.asProbability(-0.602f))
        assertEquals(exp(1.5f), AsrHypothesisConfidence.asProbability(1.5f))
    }

    @Test
    fun shouldAskRepeat_modeOffNeverDrops() {
        val policy =
            AsrGatePolicy(
                enabled = true,
                mode = AsrGateMode.OFF,
                minProb = 0.99f,
            )
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", 0.01f, policy))
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", -5.0f, policy))
    }

    @Test
    fun shouldAskRepeat_disabledNeverDrops() {
        val policy =
            AsrGatePolicy(
                enabled = false,
                mode = AsrGateMode.PROB,
                minProb = 0.99f,
            )
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", 0.01f, policy))
    }

    @Test
    fun shouldAskRepeat_modeProbComparesRawScore() {
        val policy =
            AsrGatePolicy(
                enabled = true,
                mode = AsrGateMode.PROB,
                minProb = AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB,
            )
        assertTrue(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", -0.602f, policy))
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", 0.9f, policy))
    }

    @Test
    fun shouldAskRepeat_modeLogProbAlwaysExp() {
        val policy =
            AsrGatePolicy(
                enabled = true,
                mode = AsrGateMode.LOGPROB,
                minProb = AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB,
            )
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", -0.602f, policy))
        assertTrue(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", -2.0f, policy))
    }
}
