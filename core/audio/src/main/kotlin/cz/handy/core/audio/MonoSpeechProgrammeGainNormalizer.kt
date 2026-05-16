package cz.handy.core.audio

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Krátkodobá AGC na mono PCM16 — stahuje úroveň řeči k okolí programme loudness **−23 LUFS**
 * v praxi modelovaném jako **cílový RMS −23 dBFS** vůči digitálnímu plnému rozsahu ([F2-T10]).
 *
 * Nepoužívá plné BS.1770 K-weighting (nákladné na CPU); jde o stabilní proxy vhodnou pro wake/ASR.
 * Jednovláknový writer zachovává stav bez synchronizace.
 */
class MonoSpeechProgrammeGainNormalizer(
    private val targetRmsDbFs: Double = DEFAULT_TARGET_RMS_DB_FS,
    private val silenceRmsDbFs: Double = DEFAULT_SILENCE_RMS_DB_FS,
    private val gainAttack: Double = DEFAULT_GAIN_ATTACK,
    private val gainRelease: Double = DEFAULT_GAIN_RELEASE,
    private val minLinearGain: Double = DEFAULT_MIN_LINEAR_GAIN,
    private val maxLinearGain: Double = DEFAULT_MAX_LINEAR_GAIN,
) {
    private var smoothedGainDb = 0.0

    /**
     * Upraví [chunk] na indexech `[0, length)` — stejné pole, přepsané v místě.
     */
    fun applyInPlace(
        chunk: ShortArray,
        length: Int = chunk.size,
    ) {
        require(length in 1..chunk.size)

        var sumSq = 0.0
        for (i in 0 until length) {
            val x = chunk[i] / SAMPLE_NORM
            sumSq += x * x
        }
        val rms = sqrt(sumSq / length)
        val measuredDb = 20.0 * log10(rms + RMS_EPS)

        if (measuredDb < silenceRmsDbFs) {
            smoothedGainDb *= SILENCE_GAIN_DECAY
        } else {
            val desiredGainDb = targetRmsDbFs - measuredDb
            val coeff =
                if (desiredGainDb < smoothedGainDb) {
                    gainAttack
                } else {
                    gainRelease
                }
            smoothedGainDb += coeff * (desiredGainDb - smoothedGainDb)
        }

        var g = 10.0.pow(smoothedGainDb / 20.0).coerceIn(minLinearGain, maxLinearGain)

        for (i in 0 until length) {
            val y = (chunk[i].toDouble() * g).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            chunk[i] = y.toShort()
        }
    }

    companion object {
        private const val SAMPLE_NORM = 32768.0
        private const val RMS_EPS = 1e-12
        private const val SILENCE_GAIN_DECAY = 0.995

        /** −23 dBFS RMS jako operativní cíl u řeči, v souladu se zadáním −23 LUFS ([F2-T10]). */
        const val DEFAULT_TARGET_RMS_DB_FS = -23.0

        private const val DEFAULT_SILENCE_RMS_DB_FS = -55.0
        private const val DEFAULT_GAIN_ATTACK = 0.35
        private const val DEFAULT_GAIN_RELEASE = 0.08
        private const val DEFAULT_MIN_LINEAR_GAIN = 0.05
        private const val DEFAULT_MAX_LINEAR_GAIN = 40.0
    }
}
