package cz.handy.core.common.dialog

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Orchestrace hlasového dialogu — pouze **legální** přechody mezi [DialogPhase].
 *
 * Všechny metody jsou thread-safe vůči jedné instanci (zápis do [StateFlow]).
 */
class DialogManager(
    initial: DialogPhase = DialogPhase.Idle,
) {
    private val lock = Any()

    private val _phase = MutableStateFlow(initial)
    val phase: StateFlow<DialogPhase> = _phase.asStateFlow()

    fun resetToIdle() {
        synchronized(lock) {
            _phase.value = DialogPhase.Idle
        }
    }

    /** Libovolný reset z error handleru / watchdogu. */
    fun abortToIdle() {
        resetToIdle()
    }

    fun onWakeDetected() {
        move(DialogPhase.Idle, DialogPhase.Wake)
    }

    fun onVerifyStarted() {
        move(DialogPhase.Wake, DialogPhase.Verify)
    }

    fun onVerifyPassed() {
        move(DialogPhase.Verify, DialogPhase.Capture)
    }

    fun onVerifyFailed() {
        move(DialogPhase.Verify, DialogPhase.Idle)
    }

    fun onCaptureComplete() {
        move(DialogPhase.Capture, DialogPhase.Asr)
    }

    fun onAsrComplete() {
        move(DialogPhase.Asr, DialogPhase.Nlu)
    }

    /**
     * @param requiresConfirm `true` pro destruktivní intenty → větev [DialogPhase.Confirm].
     */
    fun onNluComplete(requiresConfirm: Boolean) {
        synchronized(lock) {
            val cur = _phase.value
            if (cur != DialogPhase.Nlu) {
                throw IllegalStateException(
                    "Dialog: onNluComplete očekává ${DialogPhase.Nlu}, aktuální je $cur.",
                )
            }
            _phase.value =
                if (requiresConfirm) {
                    DialogPhase.Confirm
                } else {
                    DialogPhase.Exec
                }
        }
    }

    fun onUserConfirmed() {
        move(DialogPhase.Confirm, DialogPhase.Exec)
    }

    fun onUserRejected() {
        move(DialogPhase.Confirm, DialogPhase.Idle)
    }

    fun onExecComplete() {
        move(DialogPhase.Exec, DialogPhase.TtsAck)
    }

    fun onTtsComplete() {
        move(DialogPhase.TtsAck, DialogPhase.Idle)
    }

    private fun move(
        expected: DialogPhase,
        next: DialogPhase,
    ) {
        synchronized(lock) {
            val cur = _phase.value
            if (cur != expected) {
                throw IllegalStateException(
                    "Dialog: očekáváno $expected → $next, aktuální fáze je $cur.",
                )
            }
            _phase.value = next
        }
    }
}
