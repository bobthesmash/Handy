package cz.handy.feature.ui

import cz.handy.core.audio.EarForegroundUiState
import cz.handy.core.common.dialog.DialogPhase
import kotlin.test.Test
import kotlin.test.assertEquals

class DialogPhaseEarForegroundUiStateTest {
    @Test
    fun idleMapsToIdle() {
        assertEquals(EarForegroundUiState.Idle, DialogPhase.Idle.toEarForegroundUiState())
    }

    @Test
    fun wakeVerifyCaptureMapToListening() {
        assertEquals(EarForegroundUiState.Listening, DialogPhase.Wake.toEarForegroundUiState())
        assertEquals(EarForegroundUiState.Listening, DialogPhase.Verify.toEarForegroundUiState())
        assertEquals(EarForegroundUiState.Listening, DialogPhase.Capture.toEarForegroundUiState())
    }

    @Test
    fun asrThroughTtsAckMapToProcessing() {
        assertEquals(EarForegroundUiState.Processing, DialogPhase.Asr.toEarForegroundUiState())
        assertEquals(EarForegroundUiState.Processing, DialogPhase.Nlu.toEarForegroundUiState())
        assertEquals(EarForegroundUiState.Processing, DialogPhase.Confirm.toEarForegroundUiState())
        assertEquals(EarForegroundUiState.Processing, DialogPhase.Exec.toEarForegroundUiState())
        assertEquals(EarForegroundUiState.Processing, DialogPhase.TtsAck.toEarForegroundUiState())
    }
}
