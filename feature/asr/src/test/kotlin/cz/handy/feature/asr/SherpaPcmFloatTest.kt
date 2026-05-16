package cz.handy.feature.asr

import kotlin.test.Test
import kotlin.test.assertEquals

class SherpaPcmFloatTest {
    @Test
    fun shortToSherpaWaveform_scaling() {
        assertEquals(-1f, shortArrayOf(-32768).asSherpaWaveformMono16kHz()[0])
        assertEquals(-1f, shortArrayOf(-32767).asSherpaWaveformMono16kHz()[0], absoluteTolerance = 1e-4f)
        val one = shortArrayOf(32767).asSherpaWaveformMono16kHz()[0]
        assertEquals(32767 / SHERPA_PCM16_FLOAT_SCALE, one, absoluteTolerance = 1e-5f)
        assertEquals(0f, shortArrayOf(0).asSherpaWaveformMono16kHz()[0])
    }
}
