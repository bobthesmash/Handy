package cz.handy.feature.asr

import android.content.Context
import android.util.Log
import cz.handy.core.common.asr.PlaceAsrLanguage

/**
 * Lazy Vosk CZ recognizer for the NAVIGATE place tail (separate from command Sherpa).
 */
class CzechPlaceRecognizerHolder(
    private val appContext: Context,
) {
    private var recognizer: StreamingAsrRecognizer? = null

    fun acquire(): StreamingAsrRecognizer? {
        recognizer?.let { return it }
        val engine = createPlaceLanguageStreamingRecognizer(appContext, PlaceAsrLanguage.CZECH)
        if (engine == null) {
            Log.w(TAG, "Vosk CZ not bundled; NAVIGATE place stays on command ASR")
            return null
        }
        recognizer = engine
        return engine
    }

    fun release() {
        recognizer?.close()
        recognizer = null
    }

    private companion object {
        const val TAG = "HandyPlaceAsr"
    }
}
