package cz.handy.feature.actions.phone

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DialableNumberComposerTest {
    @Test
    fun parsesSpacedNationalNumberWithoutPlus() {
        assertEquals(
            "774912883",
            DialableNumberComposer.tryTelSpec(" 774 912 883 "),
        )
    }

    @Test
    fun acceptsInternationalPlus() {
        assertEquals(
            "+14155550100",
            DialableNumberComposer.tryTelSpec("+1 415 555 0100"),
        )
    }

    @Test
    fun acceptsCommonSeparators() {
        assertEquals(
            "+420774912883",
            DialableNumberComposer.tryTelSpec("+420 (774) 912-883"),
        )
    }

    @Test
    fun rejectsLettersInSlot() {
        assertNull(DialableNumberComposer.tryTelSpec("mamince"))
    }

    @Test
    fun rejectsTooFewDigits() {
        assertNull(DialableNumberComposer.tryTelSpec("123456"))
    }

    @Test
    fun rejectsMidStringPlus() {
        assertNull(DialableNumberComposer.tryTelSpec("420+774912883"))
    }

    @Test
    fun rejectsPlusOnly() {
        assertNull(DialableNumberComposer.tryTelSpec("+"))
    }
}
