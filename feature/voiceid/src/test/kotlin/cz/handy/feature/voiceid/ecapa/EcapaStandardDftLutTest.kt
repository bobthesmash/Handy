package cz.handy.feature.voiceid.ecapa

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EcapaStandardDftLutTest {
    @Test
    fun lut400_matchesSlowReference_zeros() {
        val frame = FloatArray(EcapaStandardDftLut.ECAPA_STANDARD_N_FFT) { 0f }
        val slow =
            EcapaStandardDftLut.fftPowerSquaredBinsSlowReference(
                frame,
                EcapaStandardDftLut.ECAPA_STANDARD_N_FFT,
            )
        val fast = EcapaStandardDftLut.fftPowerSquaredBins400(frame)
        assertContentClose(slow, fast)
    }

    @Test
    fun lut400_matchesSlowReference_pseudoNoise() {
        val rnd = Random(42)
        val frame =
            FloatArray(EcapaStandardDftLut.ECAPA_STANDARD_N_FFT) {
                rnd.nextFloat() * 2f - 1f
            }
        val slow =
            EcapaStandardDftLut.fftPowerSquaredBinsSlowReference(
                frame,
                EcapaStandardDftLut.ECAPA_STANDARD_N_FFT,
            )
        val fast = EcapaStandardDftLut.fftPowerSquaredBins400(frame)
        assertContentClose(slow, fast)
    }

    /** Relevance pro ECAPA ONNX: preprocessor musí držet přesnou shodu vstupů proti naivní cestě. */
    private fun assertContentClose(
        expected: FloatArray,
        actual: FloatArray,
    ) {
        assertEquals(expected.size, actual.size)
        expected.zip(actual).forEachIndexed { i, pair ->
            val (e, a) = pair
            val diff = kotlin.math.abs(e - a).toDouble()
            assertTrue(
                diff <= 5e-5,
                """
                měření $i: rozíl abs(diff)=$diff (lut vs naivní)
                expected=$e actual=$a
                """.trimIndent(),
            )
        }
    }
}
