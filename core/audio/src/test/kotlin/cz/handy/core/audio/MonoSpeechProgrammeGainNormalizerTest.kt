package cz.handy.core.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class MonoSpeechProgrammeGainNormalizerTest {
    @Test
    fun boostsQuietSineTowardTargetRms() {
        val n = 4096
        val freq = 7
        val fs = MicCaptureConfig.SAMPLE_RATE_HZ.toDouble()
        val chunk =
            ShortArray(n) { i ->
                val t = i / fs
                (800 * cos(2 * PI * freq * t)).toInt().coerceIn(-32768, 32767).toShort()
            }
        val rmsBefore = rmsDbFs(chunk, n)

        val norm = MonoSpeechProgrammeGainNormalizer()
        repeat(24) {
            norm.applyInPlace(chunk, n)
        }
        val rmsAfter = rmsDbFs(chunk, n)

        assertTrue(rmsBefore < MonoSpeechProgrammeGainNormalizer.DEFAULT_TARGET_RMS_DB_FS - 2.0)
        assertTrue(
            kotlin.math.abs(rmsAfter - MonoSpeechProgrammeGainNormalizer.DEFAULT_TARGET_RMS_DB_FS) < 3.5,
            "rmsAfter=$rmsAfter expected near ${MonoSpeechProgrammeGainNormalizer.DEFAULT_TARGET_RMS_DB_FS}",
        )
    }

    private fun rmsDbFs(
        s: ShortArray,
        length: Int,
    ): Double {
        var sumSq = 0.0
        for (i in 0 until length) {
            val x = s[i] / 32768.0
            sumSq += x * x
        }
        val rms = sqrt(sumSq / length)
        return 20.0 * log10(rms + 1e-12)
    }
}
