package cz.handy.feature.actions.nav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/**
 * Starts on-road turn-by-turn guidance for a spoken place ([F2-T06]).
 * Uses [MapsNavigationUris.googleNavigation] first, then Maps `dir_action=navigate`.
 * Launch goes through [LockscreenTrampolineActivity] so a locked phone can still open Maps.
 */
class MapsNavigateLauncher(
    context: Context,
) {
    private val app = context.applicationContext

    fun openPlaceQuery(place: String): Result<String> {
        val q = place.trim()
        if (q.isEmpty()) {
            return Result.failure(IllegalArgumentException("Kam navigovat?"))
        }
        val plan = MapsNavigationUris.planFor(q)
        val trampoline =
            Intent(app, LockscreenTrampolineActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(LockscreenTrampolineActivity.EXTRA_PRIMARY_URI, plan.primaryUri)
                putExtra(LockscreenTrampolineActivity.EXTRA_FALLBACK_URI, plan.fallbackUri)
                putExtra(LockscreenTrampolineActivity.EXTRA_PACKAGE, plan.mapsPackage)
            }
        return try {
            app.startActivity(trampoline)
            Result.success("Starting navigation to $q.")
        } catch (_: ActivityNotFoundException) {
            Result.failure(
                IllegalStateException("Nelze otevřít mapy (nainstalujte Google Maps nebo jinou mapovou aplikaci)."),
            )
        }
    }
}
