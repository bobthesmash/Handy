package cz.handy.core.audio

/** Mic capture tuning for `[F0-T04]` pre-roll pipeline. */
object MicCaptureConfig {
    const val SAMPLE_RATE_HZ = 16000
    const val PRE_ROLL_SECONDS = 3
    val ringCapacitySamples: Int
        get() = SAMPLE_RATE_HZ * PRE_ROLL_SECONDS
}
