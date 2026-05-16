package cz.handy.core.common.dialog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DialogManagerTest {
    @Test
    fun happyPath_withoutConfirm() {
        val dm = DialogManager()
        dm.onWakeDetected()
        dm.onVerifyStarted()
        dm.onVerifyPassed()
        dm.onCaptureComplete()
        dm.onAsrComplete()
        dm.onNluComplete(requiresConfirm = false)
        dm.onExecComplete()
        dm.onTtsComplete()
        assertEquals(DialogPhase.Idle, dm.phase.value)
    }

    @Test
    fun path_withConfirm() {
        val dm = DialogManager()
        dm.onWakeDetected()
        dm.onVerifyStarted()
        dm.onVerifyPassed()
        dm.onCaptureComplete()
        dm.onAsrComplete()
        dm.onNluComplete(requiresConfirm = true)
        assertEquals(DialogPhase.Confirm, dm.phase.value)
        dm.onUserConfirmed()
        dm.onExecComplete()
        dm.onTtsComplete()
        assertEquals(DialogPhase.Idle, dm.phase.value)
    }

    @Test
    fun confirmRejected_returnsIdle() {
        val dm = DialogManager()
        advanceToConfirm(dm)
        dm.onUserRejected()
        assertEquals(DialogPhase.Idle, dm.phase.value)
    }

    @Test
    fun verifyFailed_returnsIdle() {
        val dm = DialogManager()
        dm.onWakeDetected()
        dm.onVerifyStarted()
        dm.onVerifyFailed()
        assertEquals(DialogPhase.Idle, dm.phase.value)
    }

    @Test
    fun illegalTransition_throws() {
        val dm = DialogManager()
        assertFailsWith<IllegalStateException> {
            dm.onVerifyStarted()
        }
    }

    @Test
    fun resetToIdle_anytime() {
        val dm = DialogManager()
        dm.onWakeDetected()
        dm.resetToIdle()
        assertEquals(DialogPhase.Idle, dm.phase.value)
    }

    private fun advanceToConfirm(dm: DialogManager) {
        dm.onWakeDetected()
        dm.onVerifyStarted()
        dm.onVerifyPassed()
        dm.onCaptureComplete()
        dm.onAsrComplete()
        dm.onNluComplete(requiresConfirm = true)
    }
}
