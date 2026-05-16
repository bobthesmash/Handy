package cz.handy.core.audio

/**
 * Stav zobrazený v persistentní notifikaci foreground služby ([EarService], [F1-T20]).
 * Mapuje se z fází hlasového dialogu ve vrstvě UI (hrubě: klid / poslech řeči / zpracování).
 */
enum class EarForegroundUiState {
    Idle,
    Listening,
    Processing,
}
