package cz.handy.feature.asr

import android.content.Context
import android.util.Log
import cz.handy.core.common.asr.PlaceAsrLanguage

/**
 * Command ASR: prefer the bundled English Sherpa zipformer so English commands
 * (`navigate to`, `pause`, …) stay reliable. Vosk CZ is used for the NAVIGATE
 * place tail when [createPlaceLanguageStreamingRecognizer] asks for Czech.
 *
 * If Sherpa is missing, fall back to Vosk CZ (legacy Czech-only builds).
 */
fun createCommandStreamingAsrRecognizer(context: Context): StreamingAsrRecognizer? {
    val app = context.applicationContext
    val sherpa = createCzSherpaStreamingRecognizer(app)
    if (sherpa != null) {
        return SherpaStreamingSpeechRecognizer(sherpa)
    }
    return createVoskCzIfBundled(app)
}

fun createPlaceLanguageStreamingRecognizer(
    context: Context,
    language: PlaceAsrLanguage,
): StreamingAsrRecognizer? {
    if (language != PlaceAsrLanguage.CZECH) return null
    return createVoskCzIfBundled(context.applicationContext)
}

/**
 * Legacy: Vosk CZ first, then Sherpa. Prefer [createCommandStreamingAsrRecognizer]
 * for the always-on command mic.
 */
fun createCzStreamingAsrRecognizer(context: Context): StreamingAsrRecognizer? {
    val app = context.applicationContext
    createVoskCzIfBundled(app)?.let { return it }
    val sherpa = createCzSherpaStreamingRecognizer(app) ?: return null
    return SherpaStreamingSpeechRecognizer(sherpa)
}

private fun createVoskCzIfBundled(app: Context): StreamingAsrRecognizer? {
    if (!VoskCzModelAssets.isBundled(app)) return null
    return runCatching {
        val model = openVoskCzModel(app)
        createVoskCzStreamingRecognizer(model)
    }.onFailure {
        Log.w("HandyAsr", "Vosk CZ load failed", it)
    }.getOrNull()
}
