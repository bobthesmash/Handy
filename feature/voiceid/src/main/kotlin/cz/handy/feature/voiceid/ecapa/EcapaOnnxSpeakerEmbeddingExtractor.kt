package cz.handy.feature.voiceid.ecapa

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import cz.handy.core.audio.MicCaptureConfig
import java.nio.FloatBuffer
import java.util.Collections
import kotlin.math.sqrt

/**
 * ONNX ECAPA s výstupem **192‑D**, vstup float32 (**log‑mel**) z [`SpeechbrainEcapaPreprocessor`].
 *
 * ONNX soubor se musí shodovat s [SpeechbrainEcapaPreprocessor] a exportem embeddingu ([F1-T02]).
 */
class EcapaOnnxSpeakerEmbeddingExtractor(
    context: Context,
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment(),
    private val preprocessor: SpeechbrainEcapaPreprocessor = SpeechbrainEcapaPreprocessor(),
) : SpeakerEmbeddingExtractor {
    private val app = context.applicationContext
    private val sessionLock = Any()

    @Volatile
    private var cachedSession: OrtSession? = null

    /** Ukončí ONNX session (volitelná optimalizace paměti). */
    fun releaseSession() {
        synchronized(sessionLock) {
            cachedSession?.close()
            cachedSession = null
        }
    }

    override fun embedPcm16(pcmMono16Le: ShortArray): Result<FloatArray> =
        runCatching {
            require(pcmMono16Le.size >= SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES) {
                "PCM too short (${pcmMono16Le.size}), need ≥ ${SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES}."
            }

            synchronized(sessionLock) {
                val session = lazySession()

                val mel = preprocessor.computeLogMel(pcmMono16Le)
                if (mel.isEmpty()) {
                    error("Preprocess produced zero frames.")
                }
                val frames = mel.size / preprocessor.nMel

                val inputName = session.inputNames.first()
                val tensorInfo = session.inputInfo[inputName]?.info as? TensorInfo
                val shape = onnxInputDims(tensorInfo, frames, preprocessor.nMel)
                val buffer = reorderFeats(tensorInfo, mel, preprocessor.nMel, frames)

                OnnxTensor
                    .createTensor(
                        ortEnv,
                        FloatBuffer.wrap(buffer),
                        shape,
                    ).use { onnxIn ->
                        session.run(Collections.singletonMap(inputName, onnxIn)).use { result ->
                            check(result.size() > 0) { "Empty ONNX outputs." }
                            val outTensor = result.get(0) as OnnxTensor
                            val fb = outTensor.floatBuffer.duplicate().also { it.rewind() }
                            val raw = FloatArray(fb.remaining()).also { fb.get(it) }
                            val emb = coerceEmbedding192(raw)
                            l2Normalize(emb)
                        }
                    }
            }
        }

    private fun lazySession(): OrtSession {
        val existing = cachedSession
        if (existing != null) return existing
        if (!EcapaModelAssets.bundled(app)) {
            error("Missing ONNX in assets (${EcapaModelAssets.relativeOnnxPath()}).")
        }
        val bytes = app.assets.open(EcapaModelAssets.relativeOnnxPath()).use { it.readBytes() }
        val opts =
            OrtSession.SessionOptions().apply {
                setIntraOpNumThreads(2)
                setInterOpNumThreads(2)
            }
        val created = ortEnv.createSession(bytes, opts)
        cachedSession = created
        return created
    }

    private enum class FeedLayout {
        BatchTimeFeat,
        BatchFeatTime,
    }

    private fun onnxInputDims(
        ti: TensorInfo?,
        frames: Int,
        nMel: Int,
    ): LongArray =
        when (detectLayout(ti, nMel)) {
            FeedLayout.BatchTimeFeat -> longArrayOf(1, frames.toLong(), nMel.toLong())
            FeedLayout.BatchFeatTime -> longArrayOf(1, nMel.toLong(), frames.toLong())
        }

    private fun reorderFeats(
        ti: TensorInfo?,
        timeMajorMel: FloatArray,
        nMel: Int,
        frames: Int,
    ): FloatArray =
        when (detectLayout(ti, nMel)) {
            FeedLayout.BatchTimeFeat -> timeMajorMel.clone()
            FeedLayout.BatchFeatTime -> transposeTimeMajorToCt(timeMajorMel, nMel, frames)
        }

    private fun detectLayout(
        ti: TensorInfo?,
        nMel: Int,
    ): FeedLayout {
        val sh = ti?.shape ?: return FeedLayout.BatchTimeFeat
        if (sh.size < 3) return FeedLayout.BatchTimeFeat
        val d1 = sh[1]
        val d2 = sh.getOrNull(2) ?: -1L
        /** [1,nMel,T] výrazně rozlišíme podle známého 80 jako druhého rozměru. */
        if (d1 == nMel.toLong()) return FeedLayout.BatchFeatTime
        if (d2 == nMel.toLong()) return FeedLayout.BatchTimeFeat
        /** Symbolické rozměry (-1): preferuj nejrozšířenější [1,T,F]. */
        return FeedLayout.BatchTimeFeat
    }

    private fun transposeTimeMajorToCt(
        feats: FloatArray,
        nMel: Int,
        frames: Int,
    ): FloatArray {
        val out = FloatArray(feats.size)
        var o = 0
        for (m in 0 until nMel) {
            for (t in 0 until frames) {
                out[o++] = feats[t * nMel + m]
            }
        }
        return out
    }

    private fun coerceEmbedding192(raw: FloatArray): FloatArray {
        require(raw.size >= SpeechbrainEcapaPreprocessor.EMBEDDING_DIM) {
            "ONNX output vector too short (${raw.size}); expected ≥ ${SpeechbrainEcapaPreprocessor.EMBEDDING_DIM}."
        }
        return if (raw.size == SpeechbrainEcapaPreprocessor.EMBEDDING_DIM) {
            raw
        } else {
            raw.copyOf(SpeechbrainEcapaPreprocessor.EMBEDDING_DIM)
        }
    }

    private fun l2Normalize(v: FloatArray): FloatArray {
        var s = 0.0
        for (f in v) s += (f * f)
        val n = sqrt(s).toFloat().coerceAtLeast(1e-9f)
        return FloatArray(v.size) { idx -> v[idx] / n }
    }

    companion object {
        /** Očekávaný sample rate vstupu (shodný s enrolling recorderem). */
        const val EXPECTED_SAMPLE_RATE_HZ = MicCaptureConfig.SAMPLE_RATE_HZ
    }
}
