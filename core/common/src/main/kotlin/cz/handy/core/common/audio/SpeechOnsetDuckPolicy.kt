package cz.handy.core.common.audio

/**
 * Speech-onset media duck: pause playback for [WINDOW_MS] so ASR can hear,
 * then resume unless the command was pause/stop.
 */
class SpeechOnsetDuckPolicy {
    enum class Action {
        None,
        PauseNow,
        ResumeNow,
        KeepPaused,
    }

    data class Event(
        val action: Action,
        val generation: Int,
    )

    var isDucked: Boolean = false
        private set

    private var generation: Int = 0

    fun onSpeechHeard(playbackActive: Boolean): Event {
        if (isDucked || !playbackActive) {
            return Event(Action.None, generation)
        }
        isDucked = true
        generation += 1
        return Event(Action.PauseNow, generation)
    }

    fun onCommandResolved(keepPlaybackPaused: Boolean): Event {
        if (!isDucked) {
            return Event(Action.None, generation)
        }
        isDucked = false
        return if (keepPlaybackPaused) {
            Event(Action.KeepPaused, generation)
        } else {
            Event(Action.ResumeNow, generation)
        }
    }

    fun onWindowElapsed(expectedGeneration: Int): Event {
        if (expectedGeneration != generation || !isDucked) {
            return Event(Action.None, generation)
        }
        isDucked = false
        return Event(Action.ResumeNow, generation)
    }

    companion object {
        const val WINDOW_MS = 2_000L

        fun shouldKeepPaused(
            intentId: String,
            slots: Map<String, String>,
        ): Boolean {
            if (intentId == "STOP") return true
            if (intentId != "MEDIA_CTRL") return false
            val command = slots["command"].orEmpty().trim().lowercase()
            if (command.isBlank()) return false
            return command.contains("pause") ||
                command.contains("pauza") ||
                command.contains("stop") ||
                command.contains("zastav")
        }
    }
}
