package cz.handy.feature.wakeword

import android.content.Context

/**
 * Uživatelská citlivost wake-wordu (0…1 / klidnější až citlivější).
 * Hodnotu čte pouze [PorcupineEarWakePump] každý inference tik; při změně slideru se přestaví Porcupine.
 * Knihovní openWakeWord (`WakeWordEngine`) má vlastní práh modelu a zatím nejede přes tento store ani přes ring buffer
 * — viz ADR `0001-wake-word.md`.
 */
class WakeWordSensitivityStore(
    context: Context,
) {
    private val app = context.applicationContext
    private val prefs =
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(): Float = prefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY).coerceIn(0f, 1f)

    fun write(sensitivity: Float) {
        prefs
            .edit()
            .putFloat(KEY_SENSITIVITY, sensitivity.coerceIn(0f, 1f))
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "handy_wake_word_settings"
        const val KEY_SENSITIVITY = "keyword_sensitivity"
        const val DEFAULT_SENSITIVITY = 0.65f
    }
}
