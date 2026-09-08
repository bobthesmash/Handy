package cz.handy.core.common.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsrGateJsonTest {
    @Test
    fun parse_blankOrGarbage_returnsDefaults() {
        val expected = AsrGatePolicy.DEFAULT
        assertEquals(expected, AsrGateJson.parse(""))
        assertEquals(expected, AsrGateJson.parse("   "))
        assertEquals(expected, AsrGateJson.parse("not-json"))
        assertEquals(expected, AsrGateJson.parse("{"))
        assertEquals(expected, AsrGateJson.parse("[]"))
    }

    @Test
    fun parse_missingFileEquivalentEmptyObject_keepsBuiltInDefaults() {
        val parsed = AsrGateJson.parse("{}")
        assertEquals(AsrGatePolicy.DEFAULT, parsed)
        assertTrue(parsed.enabled)
        assertEquals(AsrGateMode.AUTO, parsed.mode)
        assertEquals(AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB, parsed.minProb)
    }

    @Test
    fun parse_fullObject() {
        val parsed =
            AsrGateJson.parse(
                """{"enabled": true, "mode": "prob", "minProb": 0.42}""",
            )
        assertTrue(parsed.enabled)
        assertEquals(AsrGateMode.PROB, parsed.mode)
        assertEquals(0.42f, parsed.minProb)
    }

    @Test
    fun parse_partialFields_fillDefaults() {
        val parsed = AsrGateJson.parse("""{"mode": "off"}""")
        assertTrue(parsed.enabled)
        assertEquals(AsrGateMode.OFF, parsed.mode)
        assertEquals(AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB, parsed.minProb)
    }

    @Test
    fun parse_enabledFalse() {
        val parsed = AsrGateJson.parse("""{"enabled": false, "mode": "logprob"}""")
        assertFalse(parsed.enabled)
        assertEquals(AsrGateMode.LOGPROB, parsed.mode)
    }

    @Test
    fun parse_unknownMode_fallsBackToAuto() {
        val parsed = AsrGateJson.parse("""{"mode": "banana"}""")
        assertEquals(AsrGateMode.AUTO, parsed.mode)
    }

    @Test
    fun parse_invalidTypes_doNotCrash() {
        val parsed =
            AsrGateJson.parse(
                """{"enabled": "yes", "mode": 1, "minProb": "hot"}""",
            )
        assertEquals(AsrGatePolicy.DEFAULT, parsed)
    }
}
