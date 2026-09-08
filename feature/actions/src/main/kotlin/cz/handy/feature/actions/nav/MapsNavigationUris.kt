package cz.handy.feature.actions.nav

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Google Maps intents that start **turn-by-turn guidance**, not a pin / route preview.
 *
 * Primary: `google.navigation:` (Maps navigation scheme).
 * Fallback: Maps URLs `dir_action=navigate` — still a navigate action, never `geo:` preview.
 */
object MapsNavigationUris {
    const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

    data class LaunchPlan(
        val primaryUri: String,
        val fallbackUri: String,
        val mapsPackage: String = GOOGLE_MAPS_PACKAGE,
        val usesLockscreenTrampoline: Boolean = true,
    )

    fun planFor(place: String): LaunchPlan {
        val q = place.trim()
        return LaunchPlan(
            primaryUri = googleNavigation(q),
            fallbackUri = mapsDirNavigate(q),
        )
    }

    fun googleNavigation(place: String): String {
        val q = encode(place.trim())
        return "google.navigation:q=$q&mode=d"
    }

    fun mapsDirNavigate(place: String): String {
        val q = encode(place.trim())
        return "https://www.google.com/maps/dir/?api=1" +
            "&destination=$q&travelmode=driving&dir_action=navigate"
    }

    fun isTurnByTurn(uri: String): Boolean {
        val u = uri.lowercase()
        if (u.startsWith("google.navigation:")) return true
        return u.contains("google.com/maps/dir") && u.contains("dir_action=navigate")
    }

    fun isPreviewOnly(uri: String): Boolean {
        val u = uri.lowercase()
        if (u.startsWith("geo:")) return true
        if (u.contains("google.com/maps/dir") && !u.contains("dir_action=navigate")) return true
        return false
    }

    private fun encode(place: String): String = URLEncoder.encode(place, StandardCharsets.UTF_8).replace("+", "%20")
}
