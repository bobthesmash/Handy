package cz.handy.feature.asr

import android.content.Context

/**
 * Lazy držák [SherpaStreamingSpeechRecognizer]: graf se vytvoří až v [acquire],
 * [release] uvolní nativní paměť (idle teardown [F3-T05]).
 */
class SherpaStreamingRecognizerHolder(
    private val appContext: Context,
) {
    private var recognizer: SherpaStreamingSpeechRecognizer? = null

    fun acquire(): SherpaStreamingSpeechRecognizer? {
        recognizer?.let { return it }
        val online = createCzSherpaStreamingRecognizer(appContext) ?: return null
        val wrapped = SherpaStreamingSpeechRecognizer(online)
        recognizer = wrapped
        return wrapped
    }

    fun peek(): SherpaStreamingSpeechRecognizer? = recognizer

    fun release() {
        recognizer?.close()
        recognizer = null
    }
}
