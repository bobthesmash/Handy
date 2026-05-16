package cz.handy.core.common.voice

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.sqrt

/**
 * Kódování **[FloatArray]** jako little-endian IEEE754 + Base64 UTF-8 řetězec ([F1‑T03]).
 */
object EmbeddingFloatCodec {
    private const val BYTE_PER_FLOAT = java.lang.Float.BYTES

    fun encodeToString(values: FloatArray): String {
        val bb =
            ByteBuffer
                .allocate(values.size * BYTE_PER_FLOAT)
                .order(ByteOrder.LITTLE_ENDIAN)
        for (f in values) {
            bb.putFloat(f)
        }
        bb.flip()
        val bytes =
            ByteArray(bb.remaining()).also {
                bb.get(it)
            }
        return Base64.getEncoder().encodeToString(bytes)
    }

    fun decodeFromBase64(encoded: String): FloatArray {
        val bytes =
            Base64.getDecoder().decode(encoded)
        if (bytes.size % BYTE_PER_FLOAT != 0) {
            throw IllegalArgumentException("Embedding byte length mismatch.")
        }
        val floats = FloatArray(bytes.size / BYTE_PER_FLOAT)
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in floats.indices) {
            floats[i] = bb.getFloat()
        }
        return floats
    }

    fun l2NormalizeInPlace(vector: FloatArray) {
        var s = 0.0
        for (f in vector) s += (f * f).toDouble()
        val n =
            sqrt(s)
                .toFloat()
                .coerceAtLeast(1e-9f)
        for (i in vector.indices) {
            vector[i] /= n
        }
    }
}
