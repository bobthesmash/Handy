package cz.handy.feature.voiceid.antispoof

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import cz.handy.feature.voiceid.ecapa.SpeechbrainEcapaPreprocessor
import cz.handy.feature.voiceid.verify.VerificationThresholdStore
import java.nio.FloatBuffer
import java.util.Collections

/**
 * Brána před ECAPA: stejný log-mel front-end jako [`EcapaOnnxSpeakerEmbeddingExtractor`].
 * ONNX musí přijmout **[1,T,80]** nebo **[1,80,T]** float32 jako ECAPA vstup (`ADR-0002`, `ADR-0007`).
 */
class AntiSpoofOnnxClassifier(
    context: Context,
    private val thresholdStore: VerificationThresholdStore,
    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment(),
    private val preprocessor: SpeechbrainEcapaPreprocessor = SpeechbrainEcapaPreprocessor(),
) {
    private val app = context.applicationContext
    private val sessionLock = Any()

    @Volatile
    private var cachedSession: OrtSession? = null

    fun releaseSession() {
        synchronized(sessionLock) {
            cachedSession?.close()
            cachedSession = null
        }
    }

    /**
     * Bez `anti_spoof.onnx` v assets je no-op úspěch.
     * Při přítomnosti modelu inference chyba = [AntiSpoofInferenceException] (fail-closed).
     */
    fun gateBeforeSpeakerVerify(pcmMono16Le: ShortArray): Result<Unit> =
        runCatching {
            if (!AntiSpoofModelAssets.bundled(app)) {
                return@runCatching
            }

            check(pcmMono16Le.size >= SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES) {
                "PCM příliš krátké (${pcmMono16Le.size}); potřeba ≥ ${SpeechbrainEcapaPreprocessor.MIN_PCM_SAMPLES}."
            }

            synchronized(sessionLock) {
                val session = lazySession()

                val mel = preprocessor.computeLogMel(pcmMono16Le)
                if (mel.isEmpty()) {
                    throw AntiSpoofInferenceException("Preprocess produced zero mel frames.")
                }
                val frames = mel.size / preprocessor.nMel
                val inputName = session.inputNames.first()
                val tensorInfo = session.inputInfo[inputName]?.info as? TensorInfo
                val shape = onnxInputDims(tensorInfo, frames, preprocessor.nMel)
                val buffer = reorderFeats(tensorInfo, mel, preprocessor.nMel, frames)

                val probSpoof =
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
                                AntiSpoofOutputParser.spoofProbabilityFromHead(raw)
                            }
                        }

                val limit = thresholdStore.read().antiSpoofRejectAbove
                if (probSpoof > limit) {
                    throw AntiSpoofRejectedException(probSpoof, limit)
                }
            }
        }

    private fun lazySession(): OrtSession {
        val existing = cachedSession
        if (existing != null) {
            return existing
        }

        check(AntiSpoofModelAssets.bundled(app)) {
            "Anti-spoof ONNX missing (${AntiSpoofModelAssets.relativeOnnxPath()})."
        }
        val bytes = app.assets.open(AntiSpoofModelAssets.relativeOnnxPath()).use { it.readBytes() }
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
        /** [1,nMel,T] od ECAPA onnxInputDims / detekční heuristika ([`EcapaOnnxSpeakerEmbeddingExtractor`]). */
        if (d1 == nMel.toLong()) return FeedLayout.BatchFeatTime
        if (d2 == nMel.toLong()) return FeedLayout.BatchTimeFeat
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
}
