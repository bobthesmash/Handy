package cz.handy.feature.actions.media

import android.util.Log
import cz.handy.core.common.audio.SpeechOnsetDuckPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pauses active media when speech is heard so Sherpa ASR is not competing with playback.
 * Tag: `HandyMediaDuck`.
 */
class SpeechOnsetMediaDucker(
    private val handover: MediaPlaybackHandover,
    private val scope: CoroutineScope,
    private val policy: SpeechOnsetDuckPolicy = SpeechOnsetDuckPolicy(),
) {
    private var windowJob: Job? = null

    fun onSpeechOnset() {
        apply(policy.onSpeechHeard(handover.isPlaybackActive()))
    }

    fun onCommandFinished(keepPlaybackPaused: Boolean) {
        apply(policy.onCommandResolved(keepPlaybackPaused))
    }

    private fun apply(event: SpeechOnsetDuckPolicy.Event) {
        when (event.action) {
            SpeechOnsetDuckPolicy.Action.None -> Unit
            SpeechOnsetDuckPolicy.Action.PauseNow -> {
                handover.pauseActivePlayback()
                Log.i(
                    TAG,
                    "paused playback for ${SpeechOnsetDuckPolicy.WINDOW_MS}ms speech window gen=${event.generation}",
                )
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
