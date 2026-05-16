package cz.handy.core.persistence

import android.os.SystemClock
import android.util.Log

/**
 * Měření [F0-T07]: wake signál → připravený Sherpa graf (a později první partial).
 * Log tag **`HandyLatency`** — pouze časové údaje a booleany, žádný obsah řeči.
 */
object PipelineLatencyTracer {
    const val LOG_TAG = "HandyLatency"

    @Volatile
    private var wakeElapsedRealtimeMs: Long? = null

    @Volatile
    private var firstPartialLogged: Boolean = false

    /** Začátek intervalu (typicky hned po detekci wake-wordu nebo simulaci). */
    fun markWakeWordSignal() {
        firstPartialLogged = false
        val t = SystemClock.elapsedRealtime()
        wakeElapsedRealtimeMs = t
        Log.i(LOG_TAG, "wake elapsedRealtimeMs=$t")
    }

    /**
     * Konec přípravy Sherpa session po wake ([SherpaStreamingRecognizerHolder.acquire]).
     */
    fun markSherpaRecognizerReady(success: Boolean) {
        val w = wakeElapsedRealtimeMs ?: return
        val now = SystemClock.elapsedRealtime()
        val delta = now - w
        if (success) {
            Log.i(LOG_TAG, "sherpa_ready wakeToReadyMs=$delta")
        } else {
            Log.w(LOG_TAG, "sherpa_ready_failed wakeToFailMs=$delta")
        }
    }

    /**
     * První streaming výstup z decoderu po wake (až bude napojen mikro-buffer).
     * [hasNonEmptyText] místo přepisu — bez PII v logu.
     */
    fun markFirstAsrPartial(hasNonEmptyText: Boolean) {
        if (firstPartialLogged) {
            return
        }
        firstPartialLogged = true
        val w = wakeElapsedRealtimeMs ?: return
        val now = SystemClock.elapsedRealtime()
        Log.i(LOG_TAG, "first_partial wakeToPartialMs=${now - w} nonEmpty=$hasNonEmptyText")
    }

    fun clearWakeAnchor() {
        wakeElapsedRealtimeMs = null
    }
}
