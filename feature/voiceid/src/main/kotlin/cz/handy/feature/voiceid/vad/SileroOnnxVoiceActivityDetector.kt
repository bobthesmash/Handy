package cz.handy.feature.voiceid.vad

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import cz.handy.core.audio.MicCaptureConfig
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Streamovací Silero VAD v5 přes ONNX Runtime — **512 vzorků @ 16 kHz** na krok ([F1‑T05]).
 *
 * Logika vstupů/výstupů kopíruje oficiální C# vzor ze `silero-vad` (`input`, `sr`, `state`
 * → `output`, `stateN`).
 */
class SileroOnnxVoiceActivityDetector(
    context: Context,
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment(),
) {
    private val app = context.applicationContext
    private val sessionLock = Any()

    @Volatile
    private var cachedSession: OrtSession? = null

    /** Konkatenovaný kontext **64** + nový úsek **512** = **576**. */
    private val concatAudio = FloatArray(CONTEXT_SAMPLES_16K + CHUNK_SAMPLES_16K)

    private val contextTail = FloatArray(CONTEXT_SAMPLES_16K)
    private val state = FloatArray(STATE_ELEMENTS)

    fun reset() {
        contextTail.fill(0f)
        state.fill(0f)
    }

    fun releaseSession() {
        synchronized(sessionLock) {
            cachedSession?.close()
            cachedSession = null
        }
    }

    /**
     * Vrací pravděpodobnost řeči pro přesně **[CHUNK_SAMPLES_16K]** mono PCM vzorků;
     * stav (**LSTM** + trailing kontext) se nese mezi voláními stejné instance.
     */
    fun probabilityFor512Pcm16Chunk(chunk512: ShortArray): Float {
        require(chunk512.size == CHUNK_SAMPLES_16K) {
            "Need exactly $CHUNK_SAMPLES_16K PCM samples per Silero ONNX step (${chunk512.size})."
        }
        synchronized(sessionLock) {
            val session = lazySession()

            contextTail.copyInto(
                concatAudio,
                destinationOffset = 0,
                startIndex = 0,
                endIndex = CONTEXT_SAMPLES_16K,
            )
            val base = CONTEXT_SAMPLES_16K
            for (i in chunk512.indices) {
                concatAudio[base + i] = chunk512[i] / SCALE_SHORT_TO_FLOAT
            }

            OnnxTensor
                .createTensor(
                    ortEnv,
                    FloatBuffer.wrap(concatAudio),
                    longArrayOf(1, concatAudio.size.toLong()),
                ).use { inputTensor ->
                    val srTensorBuffer =
                        LongBuffer.allocate(1).also {
                            it.put(MicCaptureConfig.SAMPLE_RATE_HZ.toLong())
                            it.rewind()
                        }
                    OnnxTensor
                        .createTensor(
                            ortEnv,
                            srTensorBuffer,
                            longArrayOf(1L),
                        ).use { srTensor ->
                            OnnxTensor
                                .createTensor(
                                    ortEnv,
                                    FloatBuffer.wrap(state),
                                    longArrayOf(2, 1, HID_DIM.toLong()),
                                ).use { stateTensor ->
                                    val feeds =
                                        mapOf(
                                            INPUT_NAME to inputTensor,
                                            SR_NAME to srTensor,
                                            STATE_NAME to stateTensor,
                                        )
                                    session.run(feeds).use { result ->
                                        val probTensor =
                                            result[OUTPUT_PROB_NAME] as? OnnxTensor
                                                ?: throw IllegalStateException(
                                                    "ONNX výstup „$OUTPUT_PROB_NAME“ chybí nebo není tensor.",
                                                )
                                        val prob =
                                            probTensor.floatBuffer
                                                .duplicate()
                                                .also { it.rewind() }
                                                .takeUnless { pb -> pb.remaining() == 0 }
                                                ?.get()
                                                ?: 0f

                                        val nextState =
                                            result[OUTPUT_STATE_NAME] as? OnnxTensor
                                                ?: throw IllegalStateException(
                                                    "ONNX výstup „$OUTPUT_STATE_NAME“ chybí nebo není tensor.",
                                                )
                                        val sb = nextState.floatBuffer.duplicate().also { it.rewind() }
                                        check(sb.remaining() == state.size) {
                                            "stateN má ${sb.remaining()} floatů, očekáváno ${state.size}."
                                        }
                                        sb.get(state)

                                        val ctxStart = concatAudio.size - CONTEXT_SAMPLES_16K
                                        concatAudio.copyInto(
                                            contextTail,
                                            destinationOffset = 0,
                                            startIndex = ctxStart,
                                            endIndex = concatAudio.size,
                                        )

                                        return prob
                                    }
                                }
                        }
                }
        }
    }

    private fun lazySession(): OrtSession {
        val existing = cachedSession
        if (existing != null) return existing
        if (!SileroVadModelAssets.bundled(app)) {
            error("Missing ONNX in assets (${SileroVadModelAssets.relativeOnnxPath()}).")
        }
        val bytes =
            app.assets.open(SileroVadModelAssets.relativeOnnxPath()).use { it.readBytes() }
        val opts =
            OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setInterOpNumThreads(2)
            }
        val created = ortEnv.createSession(bytes, opts)
        cachedSession = created
        return created
    }

    companion object {
        const val CHUNK_SAMPLES_16K = 512
        private const val CONTEXT_SAMPLES_16K = 64
        private const val HID_DIM = 128
        private const val STATE_ELEMENTS = 2 * 1 * HID_DIM
        private const val SCALE_SHORT_TO_FLOAT = 32768f
        private const val INPUT_NAME = "input"
        private const val SR_NAME = "sr"
        private const val STATE_NAME = "state"
        private const val OUTPUT_PROB_NAME = "output"
        private const val OUTPUT_STATE_NAME = "stateN"
    }
}
