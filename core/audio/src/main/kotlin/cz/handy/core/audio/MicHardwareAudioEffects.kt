package cz.handy.core.audio

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor

/**
 * Pokus o zapnutí HW [NoiseSuppressor] a [AcousticEchoCanceler] na audio session mikrofonu ([F2-T11]).
 * Na části zařízení není dostupné; vždy uvolnit před [android.media.AudioRecord.release].
 */
class MicHardwareAudioEffects private constructor(
    private val noiseSuppressor: NoiseSuppressor?,
    private val acousticEchoCanceler: AcousticEchoCanceler?,
) {
    fun release() {
        runCatching {
            noiseSuppressor?.enabled = false
            noiseSuppressor?.release()
        }
        runCatching {
            acousticEchoCanceler?.enabled = false
            acousticEchoCanceler?.release()
        }
    }

    companion object {
        fun tryAttach(audioSessionId: Int): MicHardwareAudioEffects {
            val ns =
                if (NoiseSuppressor.isAvailable()) {
                    NoiseSuppressor.create(audioSessionId)?.apply { enabled = true }
                } else {
                    null
                }
            val aec =
                if (AcousticEchoCanceler.isAvailable()) {
                    AcousticEchoCanceler.create(audioSessionId)?.apply { enabled = true }
                } else {
                    null
                }
            return MicHardwareAudioEffects(ns, aec)
        }
    }
}
