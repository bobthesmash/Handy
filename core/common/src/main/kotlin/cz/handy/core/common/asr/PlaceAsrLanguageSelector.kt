package cz.handy.core.common.asr

/**
 * Maps coarse country to the ASR language for a NAVIGATE `{place}` slot.
 *
 * **Precedence** (first hit wins):
 * 1. Last-known location (offline CZ/SK bounding box). If a fix exists outside CZ/SK → English.
 * 2. Telephony network country ISO (serving cell / MCC).
 * 3. SIM country ISO.
 * 4. Device locale country.
 * 5. English fallback when every signal is blank.
 *
 * Missing location permission must skip (1) and continue — never throw.
 */
object PlaceAsrLanguageSelector {
    fun select(signals: CoarseCountrySignals): PlaceAsrLanguageChoice {
        val lat = signals.lastKnownLat
        val lon = signals.lastKnownLon
        if (lat != null && lon != null) {
            val iso = CoarseCzSkBounds.isoCountry(lat, lon)
            return if (iso != null) {
                PlaceAsrLanguageChoice(PlaceAsrLanguage.CZECH, PlaceAsrLanguageSource.LAST_KNOWN_LOCATION, iso)
            } else {
                PlaceAsrLanguageChoice(PlaceAsrLanguage.ENGLISH, PlaceAsrLanguageSource.LAST_KNOWN_LOCATION, null)
            }
        }
        normalizeIso(signals.networkCountryIso)?.let { iso ->
            return fromIso(iso, PlaceAsrLanguageSource.NETWORK_COUNTRY)
        }
        normalizeIso(signals.simCountryIso)?.let { iso ->
            return fromIso(iso, PlaceAsrLanguageSource.SIM_COUNTRY)
        }
        normalizeIso(signals.localeCountryIso)?.let { iso ->
            return fromIso(iso, PlaceAsrLanguageSource.DEVICE_LOCALE)
        }
        return PlaceAsrLanguageChoice(
            language = PlaceAsrLanguage.ENGLISH,
            source = PlaceAsrLanguageSource.FALLBACK_ENGLISH,
            countryIso = null,
        )
    }

    private fun fromIso(
        iso: String,
        source: PlaceAsrLanguageSource,
    ): PlaceAsrLanguageChoice {
        val czech = iso == "CZ" || iso == "SK"
        return PlaceAsrLanguageChoice(
            language = if (czech) PlaceAsrLanguage.CZECH else PlaceAsrLanguage.ENGLISH,
            source = source,
            countryIso = iso,
        )
    }

    private fun normalizeIso(raw: String?): String? {
        val iso = raw?.trim()?.uppercase().orEmpty()
        if (iso.length != 2) return null
        if (iso.any { it !in 'A'..'Z' }) return null
        return iso
    }
}
