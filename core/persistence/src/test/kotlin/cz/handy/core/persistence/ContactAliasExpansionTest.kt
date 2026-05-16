package cz.handy.core.persistence

import kotlin.test.Test
import kotlin.test.assertEquals

class ContactAliasExpansionTest {
    @Test
    fun expandsCaseInsensitive() {
        val m = mapOf("bratr" to "Jan Novák")
        assertEquals("Jan Novák", ContactAliasExpansion.expand(m, "  Bratr  "))
    }

    @Test
    fun leavesUnknown() {
        val m = mapOf("bratr" to "Jan")
        assertEquals("máma", ContactAliasExpansion.expand(m, "máma"))
    }
}
