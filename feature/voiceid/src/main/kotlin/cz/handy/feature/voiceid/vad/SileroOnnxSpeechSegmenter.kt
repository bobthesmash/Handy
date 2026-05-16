package cz.handy.feature.voiceid.vad

import ai.onnxruntime.OrtEnvironment
import android.content.Context

private val defaultSileroMerger =
    VadSegmentMerger(
        thresholdOn = 0.55f,
        thresholdOff = 0.38f,
        minSpeechSamples = (0.16 * 16_000).toInt(),
        minTrailingSilenceSamples = (0.12 * 16_000).toInt(),
        frameHopSamples = SileroOnnxVoiceActivityDetector.CHUNK_SAMPLES_16K,
    )

/**
 * ONNX Silero segmentace řeči: načte celé PCM v násobcích **512** vzorků,
 * pak agreguje pravděpodobnosti přes [VadSegmentMerger].
 */
class SileroOnnxSpeechSegmenter(
    context: Context,
    ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment(),
    private val merger: VadSegmentMerger = defaultSileroMerger,
    private val detector: SileroOnnxVoiceActivityDetector =
        SileroOnnxVoiceActivityDetector(context, ortEnv),
) {
    fun reset() {
        detector.reset()
    }

    fun release() {
        detector.releaseSession()
    }

    /**
     * Zpracuje pouze předponu `[0 .. N)` kde `N` je největší násobek
     * [SileroOnnxVoiceActivityDetector.CHUNK_SAMPLES_16K]; zbytek vzorků se ignoruje.
     */
    fun segmentPcm16Mono(samples: ShortArray): List<VadSegment> {
        val hop = SileroOnnxVoiceActivityDetector.CHUNK_SAMPLES_16K
        if (samples.size < hop) return emptyList()
        detector.reset()
        val frames = ArrayList<VadFrame>(samples.size / hop)
        val chunk = ShortArray(hop)
        var offset = 0
        while (offset + hop <= samples.size) {
            samples.copyInto(chunk, 0, offset, offset + hop)
            val p = detector.probabilityFor512Pcm16Chunk(chunk)
            frames.add(VadFrame(offset, offset + hop, p))
            offset += hop
        }
        return merger.merge(frames)
    }
}
