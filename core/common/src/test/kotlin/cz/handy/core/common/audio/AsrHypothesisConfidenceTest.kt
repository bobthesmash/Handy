package cz.handy.core.common.audio

import kotlin.math.exp
import kotlin.math.ln
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
    fun shouldAskRepeat_logProbAboveThreshold_accepts() {
        // Device logcat: minTokenProb=-0.602… → exp(-0.602)≈0.548 > 0.28
        val logProb = -0.602f
        assertTrue(AsrHypothesisConfidence.asProbability(logProb) > AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB)
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", logProb))
    }

    @Test
    fun shouldAskRepeat_logProbBelowThreshold_rejects() {
        val logProb = ln(0.10f)
        assertTrue(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", logProb))
    }

    @Test
    fun asProbability_auto_leavesUnitIntervalUnchanged() {
        assertEquals(0.5f, AsrHypothesisConfidence.asProbability(0.5f, AsrGateMode.AUTO))
        assertEquals(0f, AsrHypothesisConfidence.asProbability(0f, AsrGateMode.AUTO))
        assertEquals(1f, AsrHypothesisConfidence.asProbability(1f, AsrGateMode.AUTO))
    }

    @Test
    fun asProbability_logprob_alwaysExp() {
        val raw = 0.5f
        assertEquals(exp(raw), AsrHypothesisConfidence.asProbability(raw, AsrGateMode.LOGPROB))
        assertEquals(raw, AsrHypothesisConfidence.asProbability(raw, AsrGateMode.OFF))
        assertEquals(raw, AsrHypothesisConfidence.asProbability(raw, AsrGateMode.PROB))
    }

    @Test
    fun shouldAskRepeat_modeOff_neverDrops() {
        val weak = AsrGatePolicy(enabled = true, mode = AsrGateMode.OFF, minProb = 0.99f)
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", 0.01f, weak))
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", -5f, weak))
    }

    @Test
    fun shouldAskRepeat_disabled_neverDrops() {
        val disabled = AsrGatePolicy(enabled = false, mode = AsrGateMode.PROB, minProb = 0.99f)
        assertFalse(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", 0.01f, disabled))
    }

    @Test
    fun shouldAskRepeat_modeProb_comparesRawNegativeAsLow() {
        val prob = AsrGatePolicy(mode = AsrGateMode.PROB, minProb = 0.28f)
        assertTrue(AsrHypothesisConfidence.shouldAskRepeat("WHAT TIME IS IT", -0.602f, prob))
    }
}
