package cz.handy.feature.wakeword

import cz.handy.core.audio.MicCaptureConfig

/**
 * Očekávaný krok mezi inferencemi wake enginu ([F3-T04]) — cílově **30 ms**, ne nepřetržitě každý vzorek.
 * Počet vzorků PCM @ 16 kHz odpovídá jednomu tiketu.
 */
object WakeWordInferenceBudget {
    const val INFERENCE_INTERVAL_MS: Int = 30

    val samplesPerTickAt16k: Int
        get() = MicCaptureConfig.SAMPLE_RATE_HZ * INFERENCE_INTERVAL_MS / 1000
}
