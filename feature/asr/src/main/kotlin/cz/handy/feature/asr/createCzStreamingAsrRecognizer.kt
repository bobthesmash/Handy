package cz.handy.feature.asr

import android.content.Context
import android.util.Log

/**
 * Preferuje český **Vosk** small; jinak Sherpa zipformer2 z assets (vývojářský náhradní jazyk).
 */
fun createCzStreamingAsrRecognizer(context: Context): StreamingAsrRecognizer? {
    val app = context.applicationContext
    if (VoskCzModelAssets.isBundled(app)) {
        return runCatching {
            val model = openVoskCzModel(app)
            createVoskCzStreamingRecognizer(model)
        }.onFailure {
            Log.w("HandyAsr", "Vosk CZ load failed, trying Sherpa fallback", it)
        }.getOrNull()
    }
    val sherpa = createCzSherpaStreamingRecognizer(app) ?: return null
    return SherpaStreamingSpeechRecognizer(sherpa)
}
