package cz.handy.feature.actions.audio

import android.content.Context
import android.media.AudioManager

/** Krokové měnění média‑stream [F1-T12]. */
class MediaVolumeAdjuster(
    context: Context,
) {
    private val am = context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun volumeUp(step: Int = 1): String {
        repeat(step.coerceAtLeast(1)) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
        }
        return "Hlasitost médií nahoru."
    }

    fun volumeDown(step: Int = 1): String {
        repeat(step.coerceAtLeast(1)) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        return "Hlasitost médií dolů."
    }

    fun muteAllMedia(): String {
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
        return "Ztišuji média."
    }
}
