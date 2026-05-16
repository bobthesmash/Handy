package cz.handy.core.common.dialog

/**
 * Hlavní fáze voice dialogu ([F1‑T07]).
 *
 * Tok: `Idle → Wake → Verify → Capture → Asr → Nlu → (Confirm?) → Exec → TtsAck → Idle`.
 */
sealed class DialogPhase {
    data object Idle : DialogPhase()

    data object Wake : DialogPhase()

    data object Verify : DialogPhase()

    /** Segment řeči zachycený pro ASR. */
    data object Capture : DialogPhase()

    /** Stream / dávkový přepis. */
    data object Asr : DialogPhase()

    /** Rule NLU / slotting ([F1‑T08]+). */
    data object Nlu : DialogPhase()

    /** Potvrzení destruktivní akce ([F1‑T16]) — volitelná větev z [Nlu]. */
    data object Confirm : DialogPhase()

    data object Exec : DialogPhase()

    /** Krátké TTS potvrzení před návratem do pasivního poslechu. */
    data object TtsAck : DialogPhase()
}
