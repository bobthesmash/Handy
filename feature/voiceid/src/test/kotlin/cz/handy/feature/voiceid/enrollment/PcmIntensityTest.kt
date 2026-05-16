package cz.handy.feature.voiceid.enrollment

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PcmIntensityTest {
    @Test
    fun silenceIsZero() {
        val buf = ShortArray(64)
        assertEquals(0f, PcmIntensity.rmsNormalized01(buf, 0, buf.size))
    }

    @Test
    fun fullScaleNonZero() {
        val buf = ShortArray(4) { Short.MAX_VALUE }
        val v = PcmIntensity.rmsNormalized01(buf, 0, buf.size)
        assertTrue(v > 0.9f)
    }
}
