package cz.handy.feature.asr

import android.content.Context

/**
 * Lazy držák streamovacího ASR (Vosk CZ preferovaně, jinak Sherpa zipformer2).
 * [release] uvolní nativní paměť (idle teardown [F3-T05]).
 */
class SherpaStreamingRecognizerHolder(
    private val appContext: Context,
) {
    private var recognizer: StreamingAsrRecognizer? = null

    fun acquire(): StreamingAsrRecognizer? {
        recognizer?.let { return it }
        val engine = createCzStreamingAsrRecognizer(appContext) ?: return null
        recognizer = engine
        return engine
    }

    fun peek(): StreamingAsrRecognizer? = recognizer

    fun release() {
        recognizer?.close()
        recognizer = null
    }
}
