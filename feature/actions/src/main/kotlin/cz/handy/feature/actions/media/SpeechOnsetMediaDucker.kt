package cz.handy.feature.actions.media

import android.os.SystemClock
import android.util.Log
import cz.handy.core.common.audio.SpeechOnsetDuckPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pauses active media when a *real* command onset is heard so Sherpa ASR is not competing
 * with playback. Music-like ASR chatter must not trigger this. Tag: `HandyMediaDuck`.
 */
class SpeechOnsetMediaDucker(
    private val handover: MediaPlaybackHandover,
    private val scope: CoroutineScope,
    private val policy: SpeechOnsetDuckPolicy = SpeechOnsetDuckPolicy(),
    private val nowMs: () -> Long = { SystemClock.elapsedRealtime() },
) {
    private var windowJob: Job? = null

    fun onSpeechOnset(
        text: String,
        minTokenProb: Float?,
        listeningForCommand: Boolean,
    ) {
        apply(
            policy.onSpeechHeard(
                SpeechOnsetDuckPolicy.OnsetSample(
                    text = text,
                    minTokenProb = minTokenProb,
                    playbackActive = handover.isPlaybackActive(),
                    listeningForCommand = listeningForCommand,
                    nowMs = nowMs(),
                ),
            ),
        )
    }

    fun onCommandFinished(keepPlaybackPaused: Boolean) {
        apply(policy.onCommandResolved(keepPlaybackPaused))
    }

    private fun apply(event: SpeechOnsetDuckPolicy.Event) {
        when (event.action) {
            SpeechOnsetDuckPolicy.Action.None -> Unit
            SpeechOnsetDuckPolicy.Action.PauseNow -> {
                when (handover.pauseActivePlayback()) {
                    DuckPauseResult.Paused ->
                        Log.i(
                            TAG,
                            "paused playback for ${SpeechOnsetDuckPolicy.WINDOW_MS}ms speech window gen=${event.generation}",
                        )
                    DuckPauseResult.NotPlaying ->
                        Log.i(TAG, "speech-onset duck skipped — nothing playing gen=${event.generation}")
                    DuckPauseResult.NoAccess ->
                        Log.e(
                            TAG,
                            "speech-onset duck cannot pause: notification listener / MEDIA_CONTENT_CONTROL missing gen=${event.generation}",
                        )
                    DuckPauseResult.NoSession ->
                        Log.w(TAG, "speech-onset duck found no media session gen=${event.generation}")
                }
                windowJob?.cancel()
                val gen = event.generation
                windowJob =
                    scope.launch {
                        delay(SpeechOnsetDuckPolicy.WINDOW_MS)
                        apply(policy.onWindowElapsed(gen))
                    }
            }
            SpeechOnsetDuckPolicy.Action.ResumeNow -> {
                windowJob?.cancel()
                handover.resumePreferredPlayback()
                Log.i(TAG, "resumed playback after speech window gen=${event.generation}")
            }
            SpeechOnsetDuckPolicy.Action.KeepPaused -> {
                windowJob?.cancel()
                handover.clearSpeechDuck()
                Log.i(TAG, "keeping playback paused (pause/stop intent) gen=${event.generation}")
            }
        }
    }

    private companion object {
        private const val TAG = "HandyMediaDuck"
    }
}
