package cz.handy.feature.voiceid.enrollment

import cz.handy.core.common.voice.EmbeddingFloatCodec

/**
 * Spojení více ECAPA výstupů (každý už je L2) do jednoho centroidu a znovunormalizace.
 */
object EnrollmentEmbeddingCentroid {
    /**
     * @param embeddings Jednotlivé vektory ze stejného rozměru (typicky už L2‑norm z ONNX).
     */
    fun mergeEmbeddings(embeddings: List<FloatArray>): FloatArray {
        require(embeddings.isNotEmpty()) {
            "Nejsou žádné embeddingy."
        }
        val dim = embeddings[0].size
        require(embeddings.all { it.size == dim }) {
            "Nesjednocený rozměr embeddingů."
        }
        val acc = FloatArray(dim)
        val n = embeddings.size.toFloat().coerceAtLeast(1f)
        for (e in embeddings) {
            for (i in acc.indices) {
                acc[i] += e[i] / n
            }
        }
        EmbeddingFloatCodec.l2NormalizeInPlace(acc)
        return acc
    }
}
