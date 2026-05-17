package cz.handy.feature.ui.prefs

import android.content.Context

/** Zapíná anglický overlay nad pravidlovým NLU ([F5-T03]). */
class AssistEnglishNluPreferences(
    context: Context,
) {
    private val app = context.applicationContext
    private val prefs =
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_EN_OVERLAY, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_EN_OVERLAY, enabled).apply()
    }

    private companion object {
        const val PREFS_NAME = "handy_f5_assist_locale"
        const val KEY_EN_OVERLAY = "english_nlu_overlay_v1"
    }
}
