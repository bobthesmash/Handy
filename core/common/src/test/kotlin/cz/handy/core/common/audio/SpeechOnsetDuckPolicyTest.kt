package cz.handy.core.common.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechOnsetDuckPolicyTest {
    @Test
    fun speech_while_playing_starts_two_second_duck() {
        val p = SpeechOnsetDuckPolicy()
        val start = p.onSpeechHeard(playbackActive = true)
        assertEquals(SpeechOnsetDuckPolicy.Action.PauseNow, start.action)
        assertTrue(p.isDucked)

        val elapsed = p.onWindowElapsed(start.generation)
        assertEquals(SpeechOnsetDuckPolicy.Action.ResumeNow, elapsed.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun pause_or_stop_intent_stays_paused() {
        val p = SpeechOnsetDuckPolicy()
        val start = p.onSpeechHeard(playbackActive = true)
        assertEquals(SpeechOnsetDuckPolicy.Action.PauseNow, start.action)

        val pause = p.onCommandResolved(keepPlaybackPaused = true)
        assertEquals(SpeechOnsetDuckPolicy.Action.KeepPaused, pause.action)
        assertFalse(p.isDucked)

        val stale = p.onWindowElapsed(start.generation)
        assertEquals(SpeechOnsetDuckPolicy.Action.None, stale.action)
    }

    @Test
    fun other_intents_resume_immediately() {
        val p = SpeechOnsetDuckPolicy()
        p.onSpeechHeard(playbackActive = true)
        val resume = p.onCommandResolved(keepPlaybackPaused = false)
        assertEquals(SpeechOnsetDuckPolicy.Action.ResumeNow, resume.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun no_duck_when_nothing_is_playing() {
        val p = SpeechOnsetDuckPolicy()
        val start = p.onSpeechHeard(playbackActive = false)
        assertEquals(SpeechOnsetDuckPolicy.Action.None, start.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun keep_paused_for_stop_and_media_pause_only() {
        assertTrue(SpeechOnsetDuckPolicy.shouldKeepPaused("STOP", emptyMap()))
        assertTrue(SpeechOnsetDuckPolicy.shouldKeepPaused("MEDIA_CTRL", mapOf("command" to "pause")))
        assertTrue(SpeechOnsetDuckPolicy.shouldKeepPaused("MEDIA_CTRL", mapOf("command" to "stop")))
        assertFalse(SpeechOnsetDuckPolicy.shouldKeepPaused("PLAY_MEDIA", emptyMap()))
        assertFalse(SpeechOnsetDuckPolicy.shouldKeepPaused("WHAT_BATTERY", emptyMap()))
        assertFalse(SpeechOnsetDuckPolicy.shouldKeepPaused("MEDIA_CTRL", mapOf("command" to "next")))
        assertFalse(SpeechOnsetDuckPolicy.shouldKeepPaused("MEDIA_CTRL", emptyMap()))
    }

    @Test
    fun already_ducked_speech_is_ignored() {
        val p = SpeechOnsetDuckPolicy()
        p.onSpeechHeard(playbackActive = true)
        val second = p.onSpeechHeard(playbackActive = true)
        assertEquals(SpeechOnsetDuckPolicy.Action.None, second.action)
        assertTrue(p.isDucked)
    }

    @Test
    fun window_ms_is_two_seconds() {
        assertEquals(2_000L, SpeechOnsetDuckPolicy.WINDOW_MS)
    }
}
