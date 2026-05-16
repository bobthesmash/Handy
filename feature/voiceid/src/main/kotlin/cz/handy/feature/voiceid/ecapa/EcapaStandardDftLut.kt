package cz.handy.feature.voiceid.ecapa

import kotlin.math.cos
import kotlin.math.sin

/**
 * Předsunutý **O(N²)** DFT jen s násobením bez opakovaných `cos/sin`.
 * Používá se pro ECAPA vstup kde `n_fft == 400` — stejná matematika jako naivní smyčka
 * (`re`/`im` v `Double`), finální energie jen u prvních `[0, nFft/2]` binů.
 */
internal object EcapaStandardDftLut {
    internal const val ECAPA_STANDARD_N_FFT = 400

    private val twoPiCoefficient = 2 * kotlin.math.PI

    private fun buildCosSinTables(
        n: Int,
        nBins: Int,
    ): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        require(nBins == n / 2 + 1)
        val twoPi = 2 * kotlin.math.PI
        val cosRows =
            Array(nBins) { k ->
                DoubleArray(n) { idx ->
                    cos(twoPi * k.toDouble() * idx.toDouble() / n.toDouble())
                }
            }
        val sinRows =
            Array(nBins) { k ->
                DoubleArray(n) { idx ->
                    sin(twoPi * k.toDouble() * idx.toDouble() / n.toDouble())
                }
            }
        return cosRows to sinRows
    }

    private val lut400Lock = Any()

    private var cos400Cache: Array<DoubleArray>? = null
    private var sin400Cache: Array<DoubleArray>? = null

    private fun lut400Bins(): Pair<Array<DoubleArray>, Array<DoubleArray>> {
        cos400Cache?.let { c ->
            sin400Cache?.let { s ->
                return c to s
            }
        }
        synchronized(lut400Lock) {
            if (cos400Cache == null || sin400Cache == null) {
                val pair = buildCosSinTables(ECAPA_STANDARD_N_FFT, ECAPA_STANDARD_N_FFT / 2 + 1)
                cos400Cache = pair.first
                sin400Cache = pair.second
            }
            return Pair(cos400Cache!!, sin400Cache!!)
        }
    }

    internal fun fftPowerSquaredBinsSlowReference(
        frameZeroPaddedToNfft: FloatArray,
        nFft: Int,
    ): FloatArray {
        require(frameZeroPaddedToNfft.size == nFft)
        val nBins = nFft / 2 + 1
        val out = FloatArray(nBins)
        val n = nFft
        for (k in 0 until nBins) {
            var re = 0.0
            var im = 0.0
            var idx = 0
            while (idx < n) {
                val angle = twoPiCoefficient * k * idx / n
                val x = frameZeroPaddedToNfft[idx].toDouble()
                re += x * cos(angle)
                im -= x * sin(angle)
                idx++
            }
            val p = (re * re + im * im).toFloat()
            out[k] = p
        }
        return out
    }

    internal fun fftPowerSquaredBins400(frameZeroPaddedTo400: FloatArray): FloatArray {
        require(frameZeroPaddedTo400.size == ECAPA_STANDARD_N_FFT)
        val nBins = ECAPA_STANDARD_N_FFT / 2 + 1
        val (cosRows, sinRows) = lut400Bins()
        val out = FloatArray(nBins)
        for (k in 0 until nBins) {
            var re = 0.0
            var im = 0.0
            val ck = cosRows[k]
            val sk = sinRows[k]
            for (idx in 0 until ECAPA_STANDARD_N_FFT) {
                val x = frameZeroPaddedTo400[idx].toDouble()
                re += x * ck[idx]
                im -= x * sk[idx]
            }
            val p = (re * re + im * im).toFloat()
            out[k] = p
        }
        return out
    }
}
