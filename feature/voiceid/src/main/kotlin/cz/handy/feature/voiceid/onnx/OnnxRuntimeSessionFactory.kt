package cz.handy.feature.voiceid.onnx

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.util.Log

/**
 * Jednotné otevření ONNX session — některé buildy (R8 / konflikt `libonnxruntime.so`) padají na
 * [OrtSession.SessionOptions]; pak použijeme výchozí options z [OrtEnvironment.createSession].
 *
 * V jednom APK nesmí být `libonnxruntime.so` z různých verzí (Sherpa 1.17.1 vs. novější ORT) —
 * JNI pak spadne na `dlopen … OrtGetApiBase` ([sherpa-onnx#566](https://github.com/k2-fsa/sherpa-onnx/issues/566)).
 */
internal object OnnxRuntimeSessionFactory {
    private const val TAG = "HandyOrtSession"

    fun openFromBytes(
        ortEnv: OrtEnvironment,
        modelBytes: ByteArray,
        intraOpThreads: Int = 2,
        interOpThreads: Int = 2,
    ): OrtSession {
        // Dotyk prostředí před SessionOptions — načte JNI knihovnu dřív než volitelná konfigurace.
        val env = ortEnv
        val options =
            runCatching {
                OrtSession.SessionOptions().apply {
                    setIntraOpNumThreads(intraOpThreads)
                    setInterOpNumThreads(interOpThreads)
                }
            }.getOrElse { cause ->
                Log.w(
                    TAG,
                    "SessionOptions unavailable (${cause.javaClass.simpleName}); using default session",
                    cause,
                )
                null
            }
        return if (options != null) {
            env.createSession(modelBytes, options)
        } else {
            env.createSession(modelBytes)
        }
    }
}
