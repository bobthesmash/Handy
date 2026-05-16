package cz.handy.feature.wakeword

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import android.app.Application
import android.os.SystemClock
import android.util.Log
import cz.handy.core.audio.EarAudioBridge
import cz.handy.core.audio.MicCaptureConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * **Porcupine** nad stejným 16 kHz mono PCM jako [cz.handy.core.audio.EarService] — bez druhého [android.media.AudioRecord].
 *
 * Inference krok odpovídá [WakeWordInferenceBudget] (~30 ms; 480 vzorků @16 kHz).
 *
 * Výchozí slovo je [Porcupine.BuiltInKeyword.PORCUPINE]; citlivost z [WakeWordSensitivityStore] se **průběžně kontroluje**
 * — při změně ve slideru přestavíme engine bez restartu aplikace (Porcupine ji neumožňuje měnit za běhu).
 *
 * Bez `picovoice.access.key` se pumpa nepouští ([ADR 0001]).
 */
object PorcupineEarWakePump {
    private const val TAG = "HandyPorcupineWake"
    private const val KEYWORD_COOLDOWN_MS = 2_800L

    /** Minimální delta v prefs, aby se vyhnulo zbytečné přestavbě kvůli šumu float uložiště. */
    private const val SENSITIVITY_REOPEN_EPSILON = 0.004f

    private var pumpJob: Job? = null

    /** Jedna instance na proces; bez klíče je no-op. */
    @Synchronized
    fun startIfAccessKeyConfigured(
        application: Application,
        scope: CoroutineScope,
    ) {
        val key = BuildConfig.PICOVOICE_ACCESS_KEY.trim()
        if (key.isEmpty()) {
            Log.i(TAG, "Pump skipped: unset picovoice.access.key.")
            return
        }

        if (pumpJob?.isActive == true) return

        pumpJob =
            scope.launch(Dispatchers.Default) {
                runPump(application, key)
            }
    }

    private fun openPorcupine(
        app: Application,
        accessKey: String,
        sensitivity: Float,
    ): Porcupine? =
        try {
            Porcupine
                .Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.PORCUPINE)
                .setSensitivity(sensitivity)
                .build(app)
        } catch (e: PorcupineException) {
            Log.w(TAG, "Porcupine open failed", e)
            null
        }

    private fun safeDeletePorcupine(instance: Porcupine?) {
        try {
            instance?.delete()
        } catch (_: PorcupineException) {
            // ignore teardown races
        }
    }

    private suspend fun runPump(
        app: Application,
        accessKey: String,
    ) {
        Log.i(
            TAG,
            "Pump start: PCM<-EarAudioBridge; tick=${WakeWordInferenceBudget.INFERENCE_INTERVAL_MS}ms.",
        )

        val sensitivityStore = WakeWordSensitivityStore(app)
        var sensitivityUsed = sensitivityStore.read()

        var porcupine: Porcupine? =
            openPorcupine(app, accessKey, sensitivityUsed)

        if (porcupine == null) return

        try {
            if (porcupine.sampleRate != MicCaptureConfig.SAMPLE_RATE_HZ) {
                Log.w(
                    TAG,
                    "Mismatch: Porcupine ${porcupine.sampleRate} Hz vs capture ${MicCaptureConfig.SAMPLE_RATE_HZ} Hz.",
                )
                return
            }

            var frameScratch = ShortArray(porcupine.frameLength)
            var acc = PorcupinePcmFrameAccumulator(porcupine.frameLength)
            var pcmMarkerExclusive = 0L
            var lastKickAtElapsed = 0L

            while (coroutineContext.isActive) {
                delay(WakeWordInferenceBudget.INFERENCE_INTERVAL_MS.toLong())

                val desiredSensitivity = sensitivityStore.read()
                if (abs(desiredSensitivity - sensitivityUsed) > SENSITIVITY_REOPEN_EPSILON) {
                    safeDeletePorcupine(porcupine)
                    val neo = openPorcupine(app, accessKey, desiredSensitivity)
                    if (neo == null) {
                        porcupine = null
                        Log.w(
                            TAG,
                            "Sensitivity reopen failed (desired=$desiredSensitivity); skipping tick.",
                        )
                        continue
                    }
                    if (neo.sampleRate != MicCaptureConfig.SAMPLE_RATE_HZ) {
                        Log.w(
                            TAG,
                            "Reopened Porcupine sampleRate=${neo.sampleRate} Hz mismatch.",
                        )
                        safeDeletePorcupine(neo)
                        return
                    }
                    porcupine = neo
                    sensitivityUsed = desiredSensitivity
                    frameScratch = ShortArray(neo.frameLength)
                    acc = PorcupinePcmFrameAccumulator(neo.frameLength)
                    pcmMarkerExclusive = 0L
                    Log.i(
                        TAG,
                        "Porcupine reopened sensitivity=${String.format(Locale.US, "%.2f", desiredSensitivity)}",
                    )
                    continue
                }

                val ring = EarAudioBridge.ringBufferOrNull()
                if (ring == null) {
                    pcmMarkerExclusive = 0L
                    continue
                }

                val activePorcupine = porcupine ?: continue

                val (fresh, watermark) =
                    ring.consumeMono16SinceTotalWritten(pcmMarkerExclusive)
                pcmMarkerExclusive = watermark
                acc.append(fresh)

                var sawWake = false
                while (acc.pollFrame(frameScratch)) {
                    val kwIndex =
                        try {
                            activePorcupine.process(frameScratch)
                        } catch (e: PorcupineException) {
                            Log.w(TAG, "Porcupine process failed", e)
                            break
                        }

                    if (kwIndex >= 0) {
                        sawWake = true
                        break
                    }
                }

                if (!sawWake) {
                    continue
                }

                val now = SystemClock.elapsedRealtime()
                if (now - lastKickAtElapsed < KEYWORD_COOLDOWN_MS) {
                    continue
                }
                lastKickAtElapsed = now
                WakeWordSignalBus.emitWakeKick()
                Log.i(TAG, "wake detected")
            }
        } finally {
            safeDeletePorcupine(porcupine)
        }
    }
}
