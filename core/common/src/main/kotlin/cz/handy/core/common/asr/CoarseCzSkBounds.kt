package cz.handy.core.common.asr

/**
 * Offline bounding boxes for Czechia and Slovakia (coarse; bike/pocket, no Geocoder).
 * Used only to pick Czech vs English place ASR — not for navigation.
 */
object CoarseCzSkBounds {
    private const val CZ_LAT_MIN = 48.55
    private const val CZ_LAT_MAX = 51.06
    private const val CZ_LON_MIN = 12.09
    private const val CZ_LON_MAX = 18.86

    private const val SK_LAT_MIN = 47.73
    private const val SK_LAT_MAX = 49.62
    private const val SK_LON_MIN = 16.83
    private const val SK_LON_MAX = 22.57

    /** ISO 3166-1 alpha-2 for CZ/SK, otherwise `null` (treat as not Czechia/Slovakia). */
    fun isoCountry(
        lat: Double,
        lon: Double,
    ): String? {
        if (inside(lat, lon, CZ_LAT_MIN, CZ_LAT_MAX, CZ_LON_MIN, CZ_LON_MAX)) return "CZ"
        if (inside(lat, lon, SK_LAT_MIN, SK_LAT_MAX, SK_LON_MIN, SK_LON_MAX)) return "SK"
        return null
    }

    private fun inside(
        lat: Double,
        lon: Double,
        latMin: Double,
        latMax: Double,
        lonMin: Double,
        lonMax: Double,
    ): Boolean = lat in latMin..latMax && lon in lonMin..lonMax
}
