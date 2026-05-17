package cz.handy.feature.asr

import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineStream
import cz.handy.core.audio.MicCaptureConfig
import cz.handy.core.common.audio.AsrHypothesisConfidence
/** Částečný výsledek jednoho vstupního PCM bloku. */
data class StreamingSherpaTick(
    val text: String,
    val endpoint: Boolean,
    /** Nejmenší `ysProbs` z posledního [OnlineRecognizer.getResult], pokud engine doplnil pole ([F2-T12]). */
    val minTokenProb: Float? = null,
)

/**
 * Tenký obal nad [OnlineRecognizer] pro streamování 16-bit mono vstupů (viz Android demo sherpa‑onnx).
 */
class SherpaStreamingSpeechRecognizer(
    private val recognizer: OnlineRecognizer,
) : StreamingAsrRecognizer {
    private var stream: OnlineStream? = null

    override fun startUtterance() {
        stream?.release()
        stream = recognizer.createStream()
    }

    /** @throws IllegalStateException pokud před zápisem PCM neproběhl [startUtterance]. */
    override fun appendPcm16Mono(pcm: ShortArray): StreamingSherpaTick {
        val st = stream ?: error("Nejprve zavolej startUtterance().")
        st.acceptWaveform(pcm.asSherpaWaveformMono16kHz(), MicCaptureConfig.SAMPLE_RATE_HZ)
        while (recognizer.isReady(st)) {
            recognizer.decode(st)
        }
        val endpoint = recognizer.isEndpoint(st)
        val result = recognizer.getResult(st)
        val minProb = AsrHypothesisConfidence.minYsProbability(result.ysProbs)
        if (endpoint) {
            recognizer.reset(st)
        }
        return StreamingSherpaTick(text = result.text, endpoint = endpoint, minTokenProb = minProb)
    }

    override fun close() {
        stream?.release()
        stream = null
        recognizer.release()
    }
}
