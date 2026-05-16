package cz.handy.feature.actions.nav

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Otevře vyhledání cíle v Google Maps přes `geo:0,0?q=…` ([F2-T06]).
 */
class MapsNavigateLauncher(
    context: Context,
) {
    private val app = context.applicationContext

    @Suppress("SameParameterValue")
    fun openPlaceQuery(place: String): Result<String> {
        val q = place.trim()
        if (q.isEmpty()) {
            return Result.failure(IllegalArgumentException("Kam navigovat?"))
        }
        val uri = Uri.parse("geo:0,0?q=${Uri.encode(q)}")
        val intent =
            Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage(GOOGLE_MAPS_PACKAGE)
            }
        return try {
            app.startActivity(intent)
            Result.success("Otevírám mapy: $q.")
        } catch (_: ActivityNotFoundException) {
            val fallback =
                Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            try {
                app.startActivity(fallback)
                Result.success("Otevírám mapy: $q.")
            } catch (_: ActivityNotFoundException) {
                Result.failure(
                    IllegalStateException("Nelze otevřít mapy (nainstalujte Google Maps nebo jinou mapovou aplikaci)."),
                )
            }
        }
    }

    companion object {
        private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
}
