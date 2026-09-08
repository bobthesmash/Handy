package cz.handy.core.common.asr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoarseCzSkBoundsTest {
    @Test
    fun prague_is_cz() {
        assertEquals("CZ", CoarseCzSkBounds.isoCountry(50.0755, 14.4378))
    }

    @Test
    fun brno_is_cz() {
        assertEquals("CZ", CoarseCzSkBounds.isoCountry(49.1951, 16.6068))
    }

    @Test
    fun bratislava_is_sk() {
        assertEquals("SK", CoarseCzSkBounds.isoCountry(48.1486, 17.1077))
    }

    @Test
    fun london_is_not_cz_sk() {
        assertNull(CoarseCzSkBounds.isoCountry(51.5074, -0.1278))
    }

    @Test
    fun new_york_is_not_cz_sk() {
        assertNull(CoarseCzSkBounds.isoCountry(40.7128, -74.0060))
    }
}
