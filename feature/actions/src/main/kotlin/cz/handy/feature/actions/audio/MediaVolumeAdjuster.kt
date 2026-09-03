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
        return "Volume up."
    }

    fun volumeDown(step: Int = 1): String {
        repeat(step.coerceAtLeast(1)) {
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
        }
        return "Volume down."
    }

    fun muteAllMedia(): String {
        am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.FLAG_SHOW_UI)
        return "Media muted."
    }

    fun adjust(operation: String): String {
        val op = operation.trim().lowercase()
        return when {
            op == "up" || op.contains("up") || op.contains("louder") || op.contains("crank") ||
                op.startsWith("zvyš") || op.startsWith("zvýš") || op.startsWith("zvěš") -> volumeUp()
            op == "down" || op.contains("down") || op.contains("quieter") || op.contains("lower") ||
                op.startsWith("sniž") || op.startsWith("sníž") -> volumeDown()
            op == "mute" || op.contains("mute") || op.contains("shut") || op.startsWith("ztiš") -> muteAllMedia()
            else -> muteAllMedia()
        }
    }
}
