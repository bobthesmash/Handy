package cz.handy.core.common.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class AsrGatePolicyJsonTest {
    @Test
    fun parseJson_nullBlankOrNonObject_returnsDefault() {
        assertSame(AsrGatePolicy.DEFAULT, AsrGatePolicy.parseJson(null))
        assertEquals(AsrGatePolicy.DEFAULT, AsrGatePolicy.parseJson(""))
        assertEquals(AsrGatePolicy.DEFAULT, AsrGatePolicy.parseJson("   "))
        assertEquals(AsrGatePolicy.DEFAULT, AsrGatePolicy.parseJson("not-json"))
        assertEquals(AsrGatePolicy.DEFAULT, AsrGatePolicy.parseJson("[1,2]"))
    }

    @Test
    fun parseJson_emptyObject_usesBuiltInDefaults() {
        val policy = AsrGatePolicy.parseJson("{}")
        assertEquals(true, policy.enabled)
        assertEquals(AsrGateMode.AUTO, policy.mode)
        assertEquals(AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB, policy.minProb, ABS_TOLERANCE)
    }

    @Test
    fun parseJson_partialFields_fillDefaults() {
        val policy = AsrGatePolicy.parseJson("""{"minProb": 0.4}""")
        assertEquals(true, policy.enabled)
        assertEquals(AsrGateMode.AUTO, policy.mode)
        assertEquals(0.4f, policy.minProb, ABS_TOLERANCE)
    }

    @Test
    fun parseJson_fullSchema() {
        val policy =
            AsrGatePolicy.parseJson(
                """
                {
                  "enabled": true,
                  "mode": "logprob",
                  "minProb": 0.15
                }
                """.trimIndent(),
            )
        assertEquals(true, policy.enabled)
        assertEquals(AsrGateMode.LOGPROB, policy.mode)
        assertEquals(0.15f, policy.minProb, ABS_TOLERANCE)
    }

    @Test
    fun parseJson_modeOffAndDisabled() {
        val off = AsrGatePolicy.parseJson("""{"mode": "OFF"}""")
        assertEquals(AsrGateMode.OFF, off.mode)
        val disabled = AsrGatePolicy.parseJson("""{"enabled": false, "mode": "prob"}""")
        assertEquals(false, disabled.enabled)
        assertEquals(AsrGateMode.PROB, disabled.mode)
        val auto = AsrGatePolicy.parseJson("""{"mode": "auto"}""")
        assertEquals(AsrGateMode.AUTO, auto.mode)
    }

    @Test
    fun modeWireNames_roundTrip() {
        AsrGateMode.entries.forEach { mode ->
            assertEquals(mode, AsrGateMode.fromWire(mode.wireName))
        }
    }

    @Test
    fun parseJson_unknownMode_keepsAutoDefault() {
        val policy = AsrGatePolicy.parseJson("""{"mode": "nope", "minProb": 0.5}""")
        assertEquals(AsrGateMode.AUTO, policy.mode)
        assertEquals(0.5f, policy.minProb, ABS_TOLERANCE)
    }

    @Test
    fun parseJson_invalidValueTypes_doNotCrash() {
        val policy = AsrGatePolicy.parseJson("""{"enabled": "yes", "mode": 1, "minProb": "hot"}""")
        assertEquals(AsrGatePolicy.DEFAULT.enabled, policy.enabled)
        assertEquals(AsrGatePolicy.DEFAULT.mode, policy.mode)
        assertEquals(AsrGatePolicy.DEFAULT.minProb, policy.minProb, ABS_TOLERANCE)
    }

    @Test
    fun parseJson_malformedObject_doesNotThrow() {
        val policy = AsrGatePolicy.parseJson("{")
        assertEquals(AsrGatePolicy.DEFAULT, policy)
    }

    companion object {
        private const val ABS_TOLERANCE = 1e-5f
    }
}
