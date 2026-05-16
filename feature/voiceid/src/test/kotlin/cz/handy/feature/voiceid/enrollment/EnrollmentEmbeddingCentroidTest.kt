package cz.handy.feature.voiceid.enrollment

import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EnrollmentEmbeddingCentroidTest {
    @Test
    fun mergeTwoNormalizesMean() {
        val a =
            floatArrayOf(3f, 4f).also {
                val n =
                    sqrt(
                        (it[0] * it[0] + it[1] * it[1]).toDouble(),
                    )
                        .toFloat()
                        .coerceAtLeast(1e-9f)
                it[0] /= n
                it[1] /= n
            }
        val b =
            floatArrayOf(0f, 1f).also {
                it[0] = 0f
                it[1] = 1f
            }

        val m = EnrollmentEmbeddingCentroid.mergeEmbeddings(listOf(a, b))
        val rawSum = floatArrayOf((a[0] + b[0]) / 2f, (a[1] + b[1]) / 2f)
        val nn =
            sqrt(
                (rawSum[0] * rawSum[0] + rawSum[1] * rawSum[1]).toDouble(),
            )
                .toFloat()
                .coerceAtLeast(1e-9f)

        assertEquals(rawSum[0] / nn, m[0], 1e-5f)
        assertEquals(rawSum[1] / nn, m[1], 1e-5f)
        val mn = kotlin.math.sqrt((m[0] * m[0] + m[1] * m[1]).toDouble()).toFloat()
        assertEquals(1f, mn, 1e-4f)
    }

    @Test
    fun rejectsEmptyList() {
        assertFailsWith<IllegalArgumentException> {
            EnrollmentEmbeddingCentroid.mergeEmbeddings(emptyList())
        }
    }
}
