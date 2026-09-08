package cz.handy.feature.actions.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MediaStopAcknowledgeTest {
    @Test
    fun successful_pause_speaks_stopped() {
        val out = MediaStopAcknowledge.of(Result.success("Pauza."))
        assertEquals(MediaStopAcknowledge.STOPPED, out.getOrThrow())
    }

    @Test
    fun no_session_is_still_stopped_not_an_error() {
        val out =
            MediaStopAcknowledge.of(
                Result.failure(IllegalStateException("Žádná aktivní mediální relace.")),
            )
        assertEquals(MediaStopAcknowledge.STOPPED, out.getOrThrow())
    }

    @Test
    fun missing_notification_listener_stays_loud() {
        val out =
            MediaStopAcknowledge.of(
                Result.failure(IllegalStateException(MediaStopAcknowledge.NLS_REQUIRED_TTS)),
            )
        assertTrue(out.isFailure)
        assertTrue(out.exceptionOrNull()!!.message!!.contains("přístup k oznámením"))
    }
}
