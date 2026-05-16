package cz.handy.feature.voiceid.verify

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Nešifrované preference pro vývojářské ladění prahů — **neobsahují embeddingy** ([F1‑T04]).
 */
class VerificationThresholdStore(
    context: Context,
) {
    private val app = context.applicationContext
    private val prefs: SharedPreferences =
        app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _thresholds =
        MutableStateFlow(readInternal())
    val thresholds: StateFlow<VerificationThresholds> = _thresholds.asStateFlow()

    fun read(): VerificationThresholds = _thresholds.value

    fun update(block: (VerificationThresholds) -> VerificationThresholds) {
        val next = block(_thresholds.value)
        writeInternal(next)
        _thresholds.value = next
    }

    fun resetToDefaults() {
        val d =
            VerificationThresholds(
                cosineHigh = VerificationThresholds.DEFAULT_COSINE_HIGH,
                cosineLow = VerificationThresholds.DEFAULT_COSINE_LOW,
            )
        writeInternal(d)
        _thresholds.value = d
    }

    private fun readInternal(): VerificationThresholds =
        VerificationThresholds(
            cosineHigh = prefs.getFloat(KEY_HIGH, VerificationThresholds.DEFAULT_COSINE_HIGH),
            cosineLow = prefs.getFloat(KEY_LOW, VerificationThresholds.DEFAULT_COSINE_LOW),
        )

    private fun writeInternal(t: VerificationThresholds) {
        prefs
            .edit()
            .putFloat(KEY_HIGH, t.cosineHigh)
            .putFloat(KEY_LOW, t.cosineLow)
            .apply()
    }

    private companion object {
        const val PREFS_NAME = "handy_voice_verify_debug"
        const val KEY_HIGH = "cosine_t_high"
        const val KEY_LOW = "cosine_t_low"
    }
}
