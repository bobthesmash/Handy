package cz.handy.feature.voiceid.ecapa

import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

class SpeechbrainEcapaPreprocessorShapeTest {
    @Test
    fun logMelProducesAtLeastOneFrame() {
        val p = SpeechbrainEcapaPreprocessor()
        val n = SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES + 4000
        val sine =
            ShortArray(n) {
                (8000 * sin(2 * kotlin.math.PI * 440.0 / 16000.0 * it)).toInt().toShort()
            }
        val mel = p.computeLogMel(sine)
        assertTrue(mel.isNotEmpty())
        assertTrue(mel.size % p.nMel == 0)
    }

    @Test
    fun melWeightsSquare() {
        val w =
            SpeechbrainEcapaPreprocessor.triangularMelWeights(
                sampleRate = 16000,
                nFft = 400,
                nMel = 80,
                fMinHz = 0f,
                fMaxHz = 8000f,
            )
        assertTrue(w.size == 201 && w.all { row -> row.size == 80 })
    }
}
