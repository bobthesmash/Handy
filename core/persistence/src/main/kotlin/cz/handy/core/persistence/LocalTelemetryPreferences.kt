package cz.handy.core.persistence

import android.content.Context

/**
 * Lokální telemetrie je defaultně vypnutá (žádný export mimo zařízení) ([F2-T13]).
 */
class LocalTelemetryPreferences(
    context: Context,
) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    private companion object {
        const val PREFS = "handy_local_telemetry"
        const val KEY_ENABLED = "enabled"
    }
}
