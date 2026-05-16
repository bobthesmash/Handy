package cz.handy.core.common.voice

import java.util.Base64
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EmbeddingFloatCodecTest {
    @Test
    fun encodeDecodeRoundtrip() {
        val v = FloatArray(192) { i -> i * 0.001f - 0.05f }
        val s = EmbeddingFloatCodec.encodeToString(v)
        val out = EmbeddingFloatCodec.decodeFromBase64(s)
        assertContentEquals(v, out)
    }

    @Test
    fun l2NormalizationIsUnitNorm() {
        val v = floatArrayOf(3f, 4f, 0f)
        EmbeddingFloatCodec.l2NormalizeInPlace(v)
        val n = sqrt(v.sumOf { (it * it).toDouble() })
        assertTrue(n in 0.99..1.01)
    }

    @Test
    fun decodeRejectsBadLength() {
        val bad =
            Base64
                .getEncoder()
                .encodeToString(byteArrayOf(1, 2, 3))
        assertFailsWith<IllegalArgumentException> {
            EmbeddingFloatCodec.decodeFromBase64(bad)
        }
    }
}
