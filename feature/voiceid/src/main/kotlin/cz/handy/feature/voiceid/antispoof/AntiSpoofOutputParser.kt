package cz.handy.feature.voiceid.antispoof

import kotlin.math.exp
import kotlin.math.max

/** Parsuje prvý ONNX výstup řady anti-spoof (logit/sigmoid / dvoutřídní softmax). Viz ADR‑0007. */
internal object AntiSpoofOutputParser {
    fun spoofProbabilityFromHead(head: FloatArray): Float {
        require(head.isNotEmpty()) { "Empty anti-spoof output head." }
        return when (head.size) {
            1 -> sigmoid(head[0])
            else -> softmaxBinarySpoofProb(head[0], head[1])
        }
    }

    private fun sigmoid(x: Float): Float {
        val d = exp((-x).toDouble()).toFloat()
        return 1f / (1f + d)
    }

    private fun softmaxBinarySpoofProb(logitBonafide: Float, logitSpoof: Float): Float {
        val m = max(logitBonafide.toDouble(), logitSpoof.toDouble())
        val e0 = exp(logitBonafide.toDouble() - m)
        val e1 = exp(logitSpoof.toDouble() - m)
        return (e1 / (e0 + e1)).toFloat()
    }
}
