package cz.handy.feature.wakeword

/**
 * Stav wake-wordu v runtime ([ADR 0001]).
 *
 * Vestavěné slovo Porcupine je anglické **„Porcupine“** (výslovně jako zvíře ježek).
 * Bez [BuildConfig.PICOVOICE_ACCESS_KEY] se [PorcupineEarWakePump] vůbec nespustí.
 */
object WakeWordAvailability {
    /** Picovoice klíč z `local.properties` → `picovoice.access.key=…` */
    fun isPicovoiceKeyConfigured(): Boolean = BuildConfig.PICOVOICE_ACCESS_KEY.trim().isNotEmpty()

    const val BUILTIN_KEYWORD_LABEL: String = "Porcupine (EN)"
}
