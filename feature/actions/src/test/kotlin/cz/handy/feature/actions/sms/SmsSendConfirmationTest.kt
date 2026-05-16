package cz.handy.feature.actions.sms

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SmsSendConfirmationTest {
    @Test
    fun allows_whenUserConfirmed() {
        val r = requireDestructiveSmsUserConfirmation(true)
        assertEquals(Result.success(Unit), r)
    }

    @Test
    fun rejects_withoutUserConfirmation() {
        val err =
            assertFailsWith<IllegalStateException> {
                requireDestructiveSmsUserConfirmation(false).getOrThrow()
            }
        assertEquals("SMS bez explicitního potvrzení uživatelem.", err.message)
    }
}
