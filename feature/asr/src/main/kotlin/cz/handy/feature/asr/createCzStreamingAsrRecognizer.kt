package cz.handy.feature.asr

import android.content.Context

/**
 * Preferuje český **Vosk** small; jinak Sherpa zipformer2 z assets (vývojářský náhradní jazyk).
 */
fun createCzStreamingAsrRecognizer(context: Context): StreamingAsrRecognizer? {
    val app = context.applicationContext
    if (VoskCzModelAssets.isBundled(app)) {
        return runCatching {
            val model = openVoskCzModel(app)
            createVoskCzStreamingRecognizer(model)
        }.getOrNull()
    }
    val sherpa = createCzSherpaStreamingRecognizer(app) ?: return null
    return SherpaStreamingSpeechRecognizer(sherpa)
}
