package cz.handy.feature.ui

import cz.handy.core.audio.EarForegroundUiState
import cz.handy.core.common.dialog.DialogPhase

internal fun DialogPhase.toEarForegroundUiState(): EarForegroundUiState =
    when (this) {
        DialogPhase.Idle -> EarForegroundUiState.Idle
        DialogPhase.Wake, DialogPhase.Verify, DialogPhase.Capture ->
            EarForegroundUiState.Listening
        DialogPhase.Asr, DialogPhase.Nlu, DialogPhase.Confirm, DialogPhase.Exec,
        DialogPhase.TtsAck,
        -> EarForegroundUiState.Processing
    }
