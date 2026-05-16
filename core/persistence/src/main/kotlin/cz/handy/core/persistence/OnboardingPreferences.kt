package cz.handy.core.persistence

import android.content.Context

/** Dokončení prvotního průvodce oprávněními ([F1-T19]). */
class OnboardingPreferences(
    context: Context,
) {
    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isPermissionsWizardComplete(): Boolean = prefs.getBoolean(KEY_DONE, false)

    fun setPermissionsWizardComplete(done: Boolean) {
        prefs.edit().putBoolean(KEY_DONE, done).apply()
    }

    private companion object {
        const val PREFS = "handy_onboarding"
        const val KEY_DONE = "permissions_wizard_v1_done"
    }
}
