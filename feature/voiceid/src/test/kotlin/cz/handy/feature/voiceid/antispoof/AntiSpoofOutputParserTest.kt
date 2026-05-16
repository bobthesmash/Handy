package cz.handy.feature.voiceid.antispoof

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AntiSpoofOutputParserTest {
    @Test
    fun `sigmoid monotone around zero`() {
        val p0 = AntiSpoofOutputParser.spoofProbabilityFromHead(floatArrayOf(0f))
        assertTrue(abs(p0 - 0.5f) < 1e-3f)

        val pPos = AntiSpoofOutputParser.spoofProbabilityFromHead(floatArrayOf(4f))
        val pNeg = AntiSpoofOutputParser.spoofProbabilityFromHead(floatArrayOf(-4f))
        assertTrue(pPos > p0 && pNeg < p0)
    }

    @Test
    fun `two-logit softmax favours spoof when second larger`() {
        val highSpoof =
            AntiSpoofOutputParser.spoofProbabilityFromHead(
                floatArrayOf(0f, 8f),
            )
        val lowSpoof =
            AntiSpoofOutputParser.spoofProbabilityFromHead(
                floatArrayOf(8f, 0f),
            )
        assertTrue(highSpoof > 0.9f)
        assertTrue(lowSpoof < 0.1f)
    }

    @Test
    fun `two-logit softmax is bounded`() {
        val p =
            AntiSpoofOutputParser.spoofProbabilityFromHead(floatArrayOf(3f, 1f))
        assertTrue(p in 0f..1f)
    }

    @Test
    fun `empty rejects`() {
        assertFailsWith<IllegalArgumentException> {
            AntiSpoofOutputParser.spoofProbabilityFromHead(floatArrayOf())
        }
    }
}
