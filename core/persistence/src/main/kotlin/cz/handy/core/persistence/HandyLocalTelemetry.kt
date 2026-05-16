package cz.handy.core.persistence

import android.content.Context
import android.os.SystemClock
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

enum class TelemetryLogClearResult {
    /** Soubor na disku nebyl přítomen. */
    WAS_MISSING,

    /** Soubor byl odstraněn. */
    CLEARED,

    /** Mazání selhalo (např. I/O na zařízení). */
    FAILED,
}

/**
 * Append-only NDJSON v app private storage — jen když [LocalTelemetryPreferences.isEnabled] ([F2-T13]).
 */
class HandyLocalTelemetry(
    context: Context,
    private val preferences: LocalTelemetryPreferences,
) {
    private val app = context.applicationContext
    private val logFile = File(app.filesDir, "handy_local_telemetry.ndjson")
    private val lock = Any()

    fun recordIntentCompleted(
        intentId: String,
        latencySinceTurnStartMs: Long,
    ) {
        append(
            JSONObject()
                .put("tWall", System.currentTimeMillis())
                .put("tBoot", SystemClock.elapsedRealtime())
                .put("kind", "intent_completed")
                .put("intentId", intentId)
                .put("latencyMs", latencySinceTurnStartMs),
        )
    }

    fun recordLowConfidenceAsrRetry() {
        append(
            JSONObject()
                .put("tWall", System.currentTimeMillis())
                .put("kind", "asr_low_confidence_retry"),
        )
    }

    /** Lokální výsledek ECAPA ověření na PCM tahu před NLU. */
    fun recordSpeakerPhraseGate(outcome: String) {
        append(
            JSONObject()
                .put("tWall", System.currentTimeMillis())
                .put("kind", "speaker_phrase_gate")
                .put("outcome", outcome),
        )
    }

    /** Lokální výsledek ONNX anti-spoof brány před ECAPA (`anti_spoof.onnx`). */
    fun recordAntiSpoofGate(outcome: String) {
        append(
            JSONObject()
                .put("tWall", System.currentTimeMillis())
                .put("kind", "anti_spoof_gate")
                .put("outcome", outcome),
        )
    }

    fun recordFalseWakeTrigger(reason: String) {
        append(
            JSONObject()
                .put("tWall", System.currentTimeMillis())
                .put("kind", "false_wake")
                .put("reason", reason),
        )
    }

    /** Lokální řádek bez textu zpětné vazby — jen hvězdy ([F4-T06], volitelné přes [LocalTelemetryPreferences]). */
    fun recordBetaFeedbackSaved(satisfactionStars: Int) {
        append(
            JSONObject()
                .put("tWall", System.currentTimeMillis())
                .put("kind", "beta_feedback_saved")
                .put("stars", satisfactionStars.coerceIn(1, 5)),
        )
    }

    /**
     * Smaže append-only NDJSON soubor v [Context.getFilesDir].
     * [LocalTelemetryPreferences] se nemění — zápis lze znovu zapnout přepínačem.
     */
    fun clearStoredLog(): TelemetryLogClearResult =
        synchronized(lock) {
            when {
                !logFile.exists() -> TelemetryLogClearResult.WAS_MISSING
                logFile.delete() -> TelemetryLogClearResult.CLEARED
                else -> TelemetryLogClearResult.FAILED
            }
        }

    /** Log existuje na disku s nenulovou velikostí — vhodné pro export. */
    fun hasExportableStoredLog(): Boolean =
        synchronized(lock) {
            logFile.exists() && logFile.length() > 0L
        }

    /** Atomické přečtení celého souboru pod zámkem (žádné další zápisy během čtení neblokují déle než trvá read). */
    fun readStoredLogBytesOrNull(): ByteArray? =
        synchronized(lock) {
            if (!logFile.exists() || logFile.length() == 0L) return@synchronized null
            logFile.readBytes()
        }

    private fun append(json: JSONObject) {
        if (!preferences.isEnabled()) return
        val line = json.toString() + "\n"
        synchronized(lock) {
            FileOutputStream(logFile, true).use { it.write(line.toByteArray(Charsets.UTF_8)) }
        }
    }
}
