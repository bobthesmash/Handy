package cz.handy.feature.actions.media

/**
 * Spoken STOP should pause media when a session is controllable, keep a quiet
 * "Stopped." when nothing is playing, and surface missing notification-listener access.
 */
object MediaStopAcknowledge {
    const val STOPPED = "Stopped."
    const val NLS_REQUIRED_TTS =
        "Povolte Handy přístup k oznámením — bez něj nelze pauznout ani zastavit hudbu."
    const val NO_SESSION_MARKER = "Žádná aktivní mediální relace"

    fun of(pause: Result<String>): Result<String> {
        if (pause.isSuccess) return Result.success(STOPPED)
        val err = pause.exceptionOrNull() ?: return Result.success(STOPPED)
        val msg = err.message.orEmpty()
        if (msg.contains(NO_SESSION_MARKER)) {
            return Result.success(STOPPED)
        }
        return Result.failure(err)
    }
}
