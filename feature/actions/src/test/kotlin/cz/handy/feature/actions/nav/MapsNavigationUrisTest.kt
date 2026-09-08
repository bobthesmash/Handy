package cz.handy.feature.actions.nav

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapsNavigationUrisTest {
    @Test
    fun google_navigation_uri_starts_driving_guidance() {
        val uri = MapsNavigationUris.googleNavigation("Austin")
        assertEquals("google.navigation:q=Austin&mode=d", uri)
        assertTrue(MapsNavigationUris.isTurnByTurn(uri))
        assertFalse(MapsNavigationUris.isPreviewOnly(uri))
    }

    @Test
    fun place_with_spaces_and_comma_is_encoded() {
        val uri = MapsNavigationUris.googleNavigation("Prague Castle, Prague")
        assertTrue(uri.startsWith("google.navigation:q="))
        assertTrue(uri.endsWith("&mode=d"))
        assertTrue(uri.contains("Prague"))
        assertFalse(uri.contains(" "))
        assertTrue(MapsNavigationUris.isTurnByTurn(uri))
    }

    @Test
    fun maps_dir_fallback_requests_navigate_not_preview() {
        val uri = MapsNavigationUris.mapsDirNavigate("Boston")
        assertTrue(uri.startsWith("https://www.google.com/maps/dir/?"))
        assertTrue(uri.contains("api=1"))
        assertTrue(uri.contains("destination=Boston"))
        assertTrue(uri.contains("travelmode=driving"))
        assertTrue(uri.contains("dir_action=navigate"))
        assertTrue(MapsNavigationUris.isTurnByTurn(uri))
        assertFalse(MapsNavigationUris.isPreviewOnly(uri))
        assertFalse(uri.contains("geo:"))
    }

    @Test
    fun launch_plan_prefers_google_navigation_and_keeps_turn_by_turn_fallback() {
        val plan = MapsNavigationUris.planFor("Austin")
        assertEquals("google.navigation:q=Austin&mode=d", plan.primaryUri)
        assertTrue(MapsNavigationUris.isTurnByTurn(plan.fallbackUri))
        assertTrue(plan.fallbackUri.contains("dir_action=navigate"))
        assertFalse(MapsNavigationUris.isPreviewOnly(plan.fallbackUri))
        assertEquals(MapsNavigationUris.GOOGLE_MAPS_PACKAGE, plan.mapsPackage)
        assertTrue(plan.usesLockscreenTrampoline)
    }

    @Test
    fun geo_search_and_dir_without_navigate_are_preview_only() {
        assertTrue(MapsNavigationUris.isPreviewOnly("geo:0,0?q=Austin"))
        assertTrue(MapsNavigationUris.isPreviewOnly("https://www.google.com/maps/dir/?api=1&destination=Austin"))
        assertFalse(MapsNavigationUris.isTurnByTurn("geo:0,0?q=Austin"))
    }

    @Test
    fun czech_place_still_starts_google_navigation_guidance() {
        val uri = MapsNavigationUris.googleNavigation("Karlův most")
        assertTrue(uri.startsWith("google.navigation:q="))
        assertTrue(uri.endsWith("&mode=d"))
        assertTrue(MapsNavigationUris.isTurnByTurn(uri))
        assertFalse(MapsNavigationUris.isPreviewOnly(uri))
        val plan = MapsNavigationUris.planFor("Karlův most")
        assertEquals(uri, plan.primaryUri)
        assertTrue(plan.fallbackUri.contains("dir_action=navigate"))
    }
}
