package cz.handy.feature.voiceid.ecapa

import cz.handy.core.common.voice.VoiceEmbeddingDimensions
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Log-mel Fbank přiblížený řetězi SpeechBrain —
 * [`Fbank`](https://github.com/speechbrain/speechbrain/blob/develop/speechbrain/lobes/features.py) +
 * [`STFT`](https://github.com/speechbrain/speechbrain/blob/develop/speechbrain/processing/features.py) +
 * default [`Filterbank`](https://github.com/speechbrain/speechbrain/blob/develop/speechbrain/processing/features.py)
 * (**n_fft=400**, Hamming jako `torch.hamming_window(M)`, **hop/win v ms**,
 * **sentence mean** jen odečtem průměru).
 *
 * Předpoklady exportu ONNX: vstup jako **jeden batch**, float32 **[1, T, 80]**,
 * kde **80** melů odpovídá **`spkrec-ecapa-voxceleb`** řadě ([F1-T02]).
 */
class SpeechbrainEcapaPreprocessor(
    private val sampleRate: Int = 16000,
    private val nFft: Int = 400,
    hopMs: Float = 10f,
    winMs: Float = 25f,
    val nMel: Int = 80,
    private val fMinHz: Float = 0f,
    private val fMaxHz: Float,
    /** SpeechBrain [`Filterbank`][amin/ref_value/top_db] defaults. */
    private val amin: Float = 1e-10f,
    private val refValue: Float = 1f,
    private val topDb: Float = 80f,
) {
    constructor(
        sampleRate: Int = 16000,
        nFft: Int = 400,
        hopMs: Float = 10f,
        winMs: Float = 25f,
        nMel: Int = 80,
        fMinHz: Float = 0f,
    ) : this(
        sampleRate = sampleRate,
        nFft = nFft,
        hopMs = hopMs,
        winMs = winMs,
        nMel = nMel,
        fMinHz = fMinHz,
        /** ECAPA pretrained default: Nyquist pokud YAML nepatří vlastní horní frekvence ([`Fbank` lobes defaults]). */
        fMaxHz = sampleRate / 2f,
    )

    private val hopSamples = ((sampleRate / 1000f) * hopMs).roundToInt().coerceAtLeast(1)
    private val winSamples = ((sampleRate / 1000f) * winMs).roundToInt().coerceIn(2, nFft)
    private val nBins = nFft / 2 + 1

    /** Mel váhy **[freqBin][mel]** v rozsahu 0…1 jako v SpeechBrain `triangular`. */
    private val melWeights: Array<FloatArray> =
        triangularMelWeights(
            sampleRate = sampleRate,
            nFft = nFft,
            nMel = nMel,
            fMinHz = fMinHz,
            fMaxHz = fMaxHz,
        )

    /** PyTorch výchozí Hamming (**periodic**). */
    private val hammingWindow: FloatArray =
        FloatArray(winSamples) { i ->
            val ph = TWO_PI_RAD * i / winSamples
            (0.54 - 0.46 * kotlin.math.cos(ph)).toFloat()
        }

    private val dbSubtract =
        (10 * kotlin.math.log10(maxOf(amin, refValue)).toDouble()).toFloat()

    /**
     * Vrátí spojité log-mel rámce (čas-major `t * [nMel] + m`).
     */
    fun computeLogMel(shorts: ShortArray): FloatArray {
        require(shorts.size >= MIN_PCM_SAMPLES) { "PCM příliš krátké (potřeba ≥ $MIN_PCM_SAMPLES vzorků @16 kHz)." }
        val waveform =
            FloatArray(shorts.size) {
                val s = shorts[it].toFloat() / SHORT_NORM
                /** Malé DC trim proti DC offsetům vstupního řetězce. */
                s
            }

        /** Constant padding jak SpeechBrain `STFT(center=True, pad_mode="constant")` — obvykle nulami. */
        val padSide = nFft / 2
        val paddedLen = waveform.size + 2 * padSide
        val padded = FloatArray(paddedLen)
        waveform.copyInto(padded, padSide)
        val nFrames = (paddedLen - winSamples) / hopSamples + 1
        if (nFrames <= 0) return FloatArray(0)

        val rawPowMel = FloatArray(nFrames * nMel)
        for (frame in 0 until nFrames) {
            val start = frame * hopSamples
            val frameSpectrum = fftPowerBins(windowedSlice(padded, start))
            for (mel in 0 until nMel) {
                var acc = 0f
                for (bin in frameSpectrum.indices) {
                    acc += frameSpectrum[bin] * melWeights[bin][mel]
                }
                rawPowMel[frame * nMel + mel] = max(acc, amin)
            }
        }

        applySpeechbrainLogCompress(rawPowMel)
        subtractSentenceMeanTimeMajor(rawPowMel, nFrames, nMel)
        return rawPowMel
    }

    /** ONNX tvar **[1, T, nMel]** pro float řádek-major buffer. */
    fun onnxFeatShape(lengthLinear: Int): LongArray {
        require(lengthLinear % nMel == 0) { "Délka nesedí na nMel=$nMel" }
        val t = lengthLinear / nMel
        return longArrayOf(1, t.toLong(), nMel.toLong())
    }

    private fun windowedSlice(
        padded: FloatArray,
        start: Int,
    ): FloatArray {
        val out = FloatArray(nFft)
        for (i in 0 until winSamples) {
            val ix = start + i
            val s =
                padded.getOrElse(ix) {
                    0f
                }
            out[i] = s * hammingWindow[i]
        }
        return out
    }

    /** Jednostranné spektrální síly $|X[k]|^2$ pro $k \in [0, n_fft/2]$. */
    private fun fftPowerBins(frameZeroPaddedToNfft: FloatArray): FloatArray {
        val out = FloatArray(nBins)
        val n = nFft
        for (k in 0 until nBins) {
            var re = 0.0
            var im = 0.0
            var idx = 0
            while (idx < n) {
                val angle = TWO_PI_RAD * k * idx / n
                val x = frameZeroPaddedToNfft[idx].toDouble()
                re += x * kotlin.math.cos(angle)
                im -= x * kotlin.math.sin(angle)
                idx++
            }
            val p = (re * re + im * im).toFloat()
            out[k] = p
        }
        return out
    }

    /** $10\cdot\log_{10}(\cdot)$ + globální strop **top_db** + odečet jako SpeechBrain `_amplitude_to_DB`. */
    private fun applySpeechbrainLogCompress(rawLinearPowMel: FloatArray) {
        var globalPeak = Float.NEGATIVE_INFINITY
        for (i in rawLinearPowMel.indices) {
            val v = 10f * log10(rawLinearPowMel[i])
            rawLinearPowMel[i] = v
            if (v > globalPeak) globalPeak = v
        }
        val floor = globalPeak - topDb
        for (i in rawLinearPowMel.indices) {
            rawLinearPowMel[i] = max(rawLinearPowMel[i], floor)
            rawLinearPowMel[i] -= dbSubtract
        }
    }

    private fun subtractSentenceMeanTimeMajor(
        feats: FloatArray,
        time: Int,
        mel: Int,
    ) {
        if (time == 0) return
        for (m in 0 until mel) {
            var sum = 0f
            for (t in 0 until time) {
                sum += feats[t * mel + m]
            }
            val mean = sum / time
            for (t in 0 until time) {
                feats[t * mel + m] -= mean
            }
        }
    }

    companion object {
        private val TWO_PI_RAD = 2 * kotlin.math.PI

        const val MIN_PCM_SAMPLES = 8000

        internal const val EMBEDDING_DIM = VoiceEmbeddingDimensions.ECAPA_V1

        private const val SHORT_NORM = 32768f

        @Suppress("MagicNumber")
        fun triangularMelWeights(
            sampleRate: Int,
            nFft: Int,
            nMel: Int,
            fMinHz: Float,
            fMaxHz: Float,
        ): Array<FloatArray> {
            fun hzToMel(hz: Float): Double = 2595.0 * kotlin.math.log10((1 + hz.toDouble() / 700.0).coerceAtLeast(1e-9))

            fun melToHz(melVal: Double): Float = (700 * (10.0.pow(melVal / 2595.0) - 1.0)).toFloat()

            val nStft = nFft / 2 + 1
            val allFreqHz =
                FloatArray(nStft) { fi ->
                    if (nStft == 1) {
                        0f
                    } else {
                        fi.toFloat() * (sampleRate / 2f) / (nStft - 1)
                    }
                }

            val melEdges = DoubleArray(nMel + 2)
            val mLow = hzToMel(fMinHz)
            val mHigh = hzToMel(fMaxHz)
            val step = (mHigh - mLow) / (nMel + 1)
            var m = mLow
            for (i in melEdges.indices) {
                melEdges[i] = m
                m += step
            }
            val hzEdges = FloatArray(nMel + 2) { melToHz(melEdges[it]) }
            val band = FloatArray(nMel)
            val central = FloatArray(nMel)
            for (k in 0 until nMel) {
                central[k] = hzEdges[k + 1]
                band[k] = maxOf(hzEdges[k + 1] - hzEdges[k], 1e-3f)
            }

            val weights = Array(nStft) { FloatArray(nMel) }
            for (fi in 0 until nStft) {
                val f = allFreqHz[fi]
                for (mel in 0 until nMel) {
                    val slope = (f - central[mel]) / band[mel]
                    val left = slope + 1f
                    val right = -slope + 1f
                    weights[fi][mel] = maxOf(0f, minOf(left, right))
                }
            }
            return weights
        }
    }
}
