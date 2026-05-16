package cz.handy.feature.wakeword

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Debug-only micro-benchmarks for `[F0-T05]`.
 *
 * - **Porcupine**: low-level [Porcupine.process] on synthetic silence (no microphone, no [PorcupineManager]).
 * - **openWakeWord** (Re-MENTIA): only checks bundled ONNX assets; real timing belongs after `RECORD_AUDIO`
 *   (see `OpenWakeWordEngineFactory` and ADR `0001-wake-word.md`).
 */
object WakeWordEnginesProbe {
    private const val TAG = "HandyWwBench"
    private const val PORCUPINE_PROCESS_ITERATIONS = 500

    suspend fun run(context: Context) {
        withContext(Dispatchers.IO) {
            benchPorcupineProcessOnly(context.applicationContext)
            logOpenWakeWordAssetStatus(context.applicationContext)
        }
    }

    private fun benchPorcupineProcessOnly(appContext: Context) {
        val key = BuildConfig.PICOVOICE_ACCESS_KEY.trim()
        if (key.isEmpty()) {
            Log.w(
                TAG,
                "Porcupine bench skipped: set picovoice.access.key in local.properties " +
                    "(or -PPICOVICE_ACCESS_KEY). Get a key at console.picovoice.ai — used for licensing, audio stays on device.",
            )
            return
        }

        var porcupine: Porcupine? = null
        try {
            porcupine =
                Porcupine
                    .Builder()
                    .setAccessKey(key)
                    .setKeyword(Porcupine.BuiltInKeyword.PORCUPINE)
                    .build(appContext)
            val frameLen = porcupine.frameLength
            val frame = ShortArray(frameLen)

            val t0 = System.nanoTime()
            repeat(PORCUPINE_PROCESS_ITERATIONS) {
                porcupine.process(frame)
            }
            val t1 = System.nanoTime()
            val avgMs = (t1 - t0) / 1_000_000.0 / PORCUPINE_PROCESS_ITERATIONS
            Log.i(
                TAG,
                "Porcupine ok: frameLength=$frameLen sampleRate=${porcupine.sampleRate} " +
                    "avgProcessMs=${"%.4f".format(avgMs)} ($PORCUPINE_PROCESS_ITERATIONS iters, silence)",
            )
        } catch (e: PorcupineException) {
            Log.w(TAG, "Porcupine init/process failed", e)
        } finally {
            try {
                porcupine?.delete()
            } catch (_: PorcupineException) {
                // ignore
            }
        }
    }

    private fun logOpenWakeWordAssetStatus(appContext: Context) {
        if (!OpenWakeWordEngineFactory.hasBundledModels(appContext)) {
            Log.w(
                TAG,
                "openWakeWord (Re-MENTIA) ONNX bundle missing — add ${OpenWakeWordEngineFactory.REQUIRED_ASSETS.joinToString()} " +
                    "under assets/${OpenWakeWordEngineFactory.ASSET_DIR}/ (see module assets README + ADR 0001).",
            )
            return
        }
        Log.i(
            TAG,
            "openWakeWord ONNX assets present — call ${OpenWakeWordEngineFactory::class.simpleName}.createEngine() " +
                "after RECORD_AUDIO to run live detection (not started from Application).",
        )
    }
}
