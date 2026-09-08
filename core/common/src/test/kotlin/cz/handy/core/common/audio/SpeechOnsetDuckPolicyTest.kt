package cz.handy.core.common.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechOnsetDuckPolicyTest {
    @Test
    fun speech_while_playing_starts_two_second_duck() {
        val p = SpeechOnsetDuckPolicy()
        val start = p.hearCommand("pause")
        assertEquals(SpeechOnsetDuckPolicy.Action.PauseNow, start.action)
        assertTrue(p.isDucked)

        val elapsed = p.onWindowElapsed(start.generation)
        assertEquals(SpeechOnsetDuckPolicy.Action.ResumeNow, elapsed.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun pause_or_stop_intent_stays_paused() {
        val p = SpeechOnsetDuckPolicy()
        val start = p.hearCommand("pause")
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
        p.hearCommand("battery")
        val resume = p.onCommandResolved(keepPlaybackPaused = false)
        assertEquals(SpeechOnsetDuckPolicy.Action.ResumeNow, resume.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun no_duck_when_nothing_is_playing() {
        val p = SpeechOnsetDuckPolicy()
        val start =
            p.onSpeechHeard(
                commandOnset("pause", playbackActive = false),
            )
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
        assertFalse(SpeechOnsetDuckPolicy.shouldKeepPaused("VOLUME", mapOf("operation" to "mute")))
    }

    @Test
    fun already_ducked_speech_is_ignored() {
        val p = SpeechOnsetDuckPolicy()
        p.hearCommand("pause")
        val second = p.hearCommand("pause", nowMs = 80L)
        assertEquals(SpeechOnsetDuckPolicy.Action.None, second.action)
        assertTrue(p.isDucked)
    }

    @Test
    fun window_ms_is_two_seconds() {
        assertEquals(2_000L, SpeechOnsetDuckPolicy.WINDOW_MS)
    }

    @Test
    fun music_like_rapid_low_confidence_onsets_do_not_pause() {
        val p = SpeechOnsetDuckPolicy()
        val first =
            p.onSpeechHeard(
                musicOnset("AND A GOLD BRIMUS", minTokenProb = 0.12f, nowMs = 1_000L),
            )
        val second =
            p.onSpeechHeard(
                musicOnset("GO THEY MUSTN'T", minTokenProb = 0.09f, nowMs = 1_040L),
            )
        val third =
            p.onSpeechHeard(
                musicOnset("AND A GOLD BRIMUS", minTokenProb = 0.11f, nowMs = 1_080L),
            )
        assertEquals(SpeechOnsetDuckPolicy.Action.None, first.action)
        assertEquals(SpeechOnsetDuckPolicy.Action.None, second.action)
        assertEquals(SpeechOnsetDuckPolicy.Action.None, third.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun music_like_rapid_distinct_lyrics_do_not_pause_even_if_confident() {
        val p = SpeechOnsetDuckPolicy()
        p.onSpeechHeard(musicOnset("AND A GOLD BRIMUS", minTokenProb = 0.71f, nowMs = 2_000L))
        p.onSpeechHeard(musicOnset("GO THEY MUSTN'T", minTokenProb = 0.68f, nowMs = 2_050L))
        val third =
            p.onSpeechHeard(
                musicOnset("THEY RUN THE HILL", minTokenProb = 0.74f, nowMs = 2_090L),
            )
        assertEquals(SpeechOnsetDuckPolicy.Action.None, third.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun real_pause_command_onset_still_ducks() {
        val p = SpeechOnsetDuckPolicy()
        val start = p.hearCommand("pause", nowMs = 8_000L)
        assertEquals(SpeechOnsetDuckPolicy.Action.PauseNow, start.action)
        assertTrue(p.isDucked)
    }

    @Test
    fun growing_command_partial_is_not_treated_as_music_chatter() {
        val p = SpeechOnsetDuckPolicy()
        p.onSpeechHeard(commandOnset("pau", minTokenProb = 0.82f, nowMs = 9_000L))
        val ducked =
            p.onSpeechHeard(
                commandOnset("pause", minTokenProb = 0.88f, nowMs = 9_040L),
            )
        assertEquals(SpeechOnsetDuckPolicy.Action.PauseNow, ducked.action)
        assertTrue(p.isDucked)
    }

    @Test
    fun standby_or_tts_is_not_listening_for_command_so_music_cannot_duck() {
        val p = SpeechOnsetDuckPolicy()
        val start =
            p.onSpeechHeard(
                commandOnset("pause", listeningForCommand = false, nowMs = 3_000L),
            )
        assertEquals(SpeechOnsetDuckPolicy.Action.None, start.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun short_lyric_words_do_not_duck_even_when_confident() {
        val p = SpeechOnsetDuckPolicy()
        for (lyric in listOf("baby", "yeah", "love", "tonight", "mute")) {
            val event =
                p.onSpeechHeard(
                    commandOnset(lyric, minTokenProb = 0.92f, nowMs = 5_000L),
                )
            assertEquals(SpeechOnsetDuckPolicy.Action.None, event.action, lyric)
        }
        assertFalse(p.isDucked)
    }

    @Test
    fun pause_still_ducks_when_sherpa_omits_token_probs() {
        val p = SpeechOnsetDuckPolicy()
        val start =
            p.onSpeechHeard(
                commandOnset("pause", minTokenProb = null, nowMs = 6_000L),
            )
        assertEquals(SpeechOnsetDuckPolicy.Action.PauseNow, start.action)
        assertTrue(p.isDucked)
    }

    @Test
    fun lyric_line_without_token_probs_does_not_duck() {
        val p = SpeechOnsetDuckPolicy()
        val event =
            p.onSpeechHeard(
                musicOnset("AND A GOLD BRIMUS", minTokenProb = null, nowMs = 7_000L),
            )
        assertEquals(SpeechOnsetDuckPolicy.Action.None, event.action)
        assertFalse(p.isDucked)
    }

    @Test
    fun short_or_low_confidence_onsets_are_ignored() {
        val p = SpeechOnsetDuckPolicy()
        val shortText =
            p.onSpeechHeard(
                commandOnset("an", minTokenProb = 0.95f, nowMs = 4_000L),
            )
        val lowConf =
            p.onSpeechHeard(
                commandOnset("pause", minTokenProb = 0.12f, nowMs = 4_040L),
            )
        assertEquals(SpeechOnsetDuckPolicy.Action.None, shortText.action)
        assertEquals(SpeechOnsetDuckPolicy.Action.None, lowConf.action)
        assertFalse(p.isDucked)
    }

    private fun SpeechOnsetDuckPolicy.hearCommand(
        text: String,
        nowMs: Long = 0L,
    ) = onSpeechHeard(commandOnset(text, nowMs = nowMs))

    private fun commandOnset(
        text: String,
        playbackActive: Boolean = true,
        listeningForCommand: Boolean = true,
        minTokenProb: Float? = 0.9f,
        nowMs: Long = 0L,
    ) = SpeechOnsetDuckPolicy.OnsetSample(
        text = text,
        minTokenProb = minTokenProb,
        playbackActive = playbackActive,
        listeningForCommand = listeningForCommand,
        nowMs = nowMs,
    )

    private fun musicOnset(
        text: String,
        minTokenProb: Float?,
        nowMs: Long,
    ) = SpeechOnsetDuckPolicy.OnsetSample(
        text = text,
        minTokenProb = minTokenProb,
        playbackActive = true,
        listeningForCommand = true,
        nowMs = nowMs,
    )
}
