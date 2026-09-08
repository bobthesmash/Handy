package cz.handy.core.common.asr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlaceAsrLanguageSelectorTest {
    @Test
    fun last_known_in_czechia_selects_czech() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(
                    lastKnownLat = 50.0755,
                    lastKnownLon = 14.4378,
                    locationPermissionGranted = true,
                ),
            )
        assertEquals(PlaceAsrLanguage.CZECH, choice.language)
        assertEquals(PlaceAsrLanguageSource.LAST_KNOWN_LOCATION, choice.source)
        assertEquals("CZ", choice.countryIso)
    }

    @Test
    fun last_known_in_slovakia_selects_czech() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(
                    lastKnownLat = 48.1486,
                    lastKnownLon = 17.1077,
                    locationPermissionGranted = true,
                ),
            )
        assertEquals(PlaceAsrLanguage.CZECH, choice.language)
        assertEquals(PlaceAsrLanguageSource.LAST_KNOWN_LOCATION, choice.source)
        assertEquals("SK", choice.countryIso)
    }

    @Test
    fun last_known_in_london_selects_english_even_with_czech_sim() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(
                    lastKnownLat = 51.5074,
                    lastKnownLon = -0.1278,
                    locationPermissionGranted = true,
                    networkCountryIso = "CZ",
                    simCountryIso = "CZ",
                    localeCountryIso = "CZ",
                ),
            )
        assertEquals(PlaceAsrLanguage.ENGLISH, choice.language)
        assertEquals(PlaceAsrLanguageSource.LAST_KNOWN_LOCATION, choice.source)
        assertNull(choice.countryIso)
    }

    @Test
    fun missing_location_permission_uses_network_country() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(
                    locationPermissionGranted = false,
                    lastKnownUnavailableReason = "ACCESS_COARSE_LOCATION not granted",
                    networkCountryIso = "cz",
                    simCountryIso = "US",
                    localeCountryIso = "GB",
                ),
            )
        assertEquals(PlaceAsrLanguage.CZECH, choice.language)
        assertEquals(PlaceAsrLanguageSource.NETWORK_COUNTRY, choice.source)
        assertEquals("CZ", choice.countryIso)
    }

    @Test
    fun network_gb_selects_english_before_czech_sim() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(
                    locationPermissionGranted = false,
                    networkCountryIso = "GB",
                    simCountryIso = "CZ",
                    localeCountryIso = "CZ",
                ),
            )
        assertEquals(PlaceAsrLanguage.ENGLISH, choice.language)
        assertEquals(PlaceAsrLanguageSource.NETWORK_COUNTRY, choice.source)
        assertEquals("GB", choice.countryIso)
    }

    @Test
    fun sim_iso_used_when_network_blank() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(
                    networkCountryIso = " ",
                    simCountryIso = "sk",
                    localeCountryIso = "US",
                ),
            )
        assertEquals(PlaceAsrLanguage.CZECH, choice.language)
        assertEquals(PlaceAsrLanguageSource.SIM_COUNTRY, choice.source)
        assertEquals("SK", choice.countryIso)
    }

    @Test
    fun device_locale_used_when_telephony_missing() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(localeCountryIso = "CZ"),
            )
        assertEquals(PlaceAsrLanguage.CZECH, choice.language)
        assertEquals(PlaceAsrLanguageSource.DEVICE_LOCALE, choice.source)
        assertEquals("CZ", choice.countryIso)
    }

    @Test
    fun empty_signals_fall_back_to_english() {
        val choice = PlaceAsrLanguageSelector.select(CoarseCountrySignals())
        assertEquals(PlaceAsrLanguage.ENGLISH, choice.language)
        assertEquals(PlaceAsrLanguageSource.FALLBACK_ENGLISH, choice.source)
        assertNull(choice.countryIso)
    }

    @Test
    fun last_known_without_fix_does_not_win_over_network() {
        val choice =
            PlaceAsrLanguageSelector.select(
                CoarseCountrySignals(
                    locationPermissionGranted = true,
                    lastKnownUnavailableReason = "no last-known location",
                    networkCountryIso = "US",
                ),
            )
        assertEquals(PlaceAsrLanguage.ENGLISH, choice.language)
        assertEquals(PlaceAsrLanguageSource.NETWORK_COUNTRY, choice.source)
        assertEquals("US", choice.countryIso)
    }
}
