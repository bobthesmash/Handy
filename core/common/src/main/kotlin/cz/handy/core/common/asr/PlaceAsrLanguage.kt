package cz.handy.core.common.asr

/**
 * Language used to recognize the `{place}` slot after an English `navigate to` command.
 *
 * Commands stay on the English command ASR; only the spoken place tail follows this choice.
 */
enum class PlaceAsrLanguage {
    CZECH,
    ENGLISH,
}

enum class PlaceAsrLanguageSource {
    LAST_KNOWN_LOCATION,
    NETWORK_COUNTRY,
    SIM_COUNTRY,
    DEVICE_LOCALE,
    FALLBACK_ENGLISH,
}

/**
 * Coarse country clues for [PlaceAsrLanguageSelector].
 *
 * Precedence is applied in the selector, not here:
 * last-known lat/lon → network ISO → SIM ISO → device locale → English.
 */
data class CoarseCountrySignals(
    val lastKnownLat: Double? = null,
    val lastKnownLon: Double? = null,
    val locationPermissionGranted: Boolean = false,
    val lastKnownUnavailableReason: String? = null,
    val networkCountryIso: String? = null,
    val simCountryIso: String? = null,
    val localeCountryIso: String? = null,
)

data class PlaceAsrLanguageChoice(
    val language: PlaceAsrLanguage,
    val source: PlaceAsrLanguageSource,
    val countryIso: String?,
)
