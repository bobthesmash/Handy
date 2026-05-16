package cz.handy.feature.actions.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import java.util.Locale

/**
 * Spustí nainstalovanou aplikaci podle řečového aliasu nebo přímého package name ([F2-T05]).
 */
class OpenAppLauncher(
    context: Context,
) {
    private val app = context.applicationContext
    private val pm = app.packageManager
    private val czechLocale = Locale.forLanguageTag("cs-CZ")

    private val aliasToPackage: Map<String, String> =
        mapOf(
            "mapy" to "com.google.android.apps.maps",
            "google mapy" to "com.google.android.apps.maps",
            "navigace" to "com.google.android.apps.maps",
            "chrome" to "com.android.chrome",
            "prohlížeč" to "com.android.chrome",
            "youtube" to "com.google.android.youtube",
            "nastavení" to "com.android.settings",
            "kalendář" to "com.google.android.calendar",
            "hodiny" to "com.google.android.deskclock",
            "budík" to "com.google.android.deskclock",
            "zprávy" to "com.google.android.apps.messaging",
            "sms" to "com.google.android.apps.messaging",
            "gmail" to "com.google.android.gm",
            "kontakty" to "com.google.android.contacts",
            "kamera" to "com.android.camera2",
            "fotoaparát" to "com.android.camera2",
            "galerie" to "com.google.android.apps.photos",
            "spotify" to "com.spotify.music",
            "hudební přehrávač" to "com.google.android.music",
        )

    fun openBySpeechAlias(userInput: String): Result<String> {
        val raw = userInput.trim()
        if (raw.isEmpty()) {
            return Result.failure(IllegalArgumentException("Jakou aplikaci otevřít?"))
        }
        val key = raw.lowercase()
        if ('.' in raw && pm.getLaunchIntentForPackage(raw) != null) {
            return startPackage(raw, label = raw)
        }
        aliasToPackage[key]?.let { return startPackage(it, friendlyLabel(key, it)) }
        val byPhrase =
            aliasToPackage.entries
                .sortedByDescending { it.key.length }
                .find { (alias, _) -> key.contains(alias) }
        if (byPhrase != null) {
            return startPackage(byPhrase.value, friendlyLabel(byPhrase.key, byPhrase.value))
        }
        return Result.failure(
            IllegalStateException("Neznámý název aplikace: „$raw“. Zkusit říct například mapy, chrome nebo nastavení."),
        )
    }

    private fun friendlyLabel(
        alias: String,
        pkg: String,
    ): String =
        when {
            alias.length <= 24 -> alias.replaceFirstChar { it.titlecase(czechLocale) }
            else -> pkg
        }

    private fun startPackage(
        packageName: String,
        label: String,
    ): Result<String> {
        val launch =
            pm.getLaunchIntentForPackage(packageName)
                ?: return Result.failure(
                    IllegalStateException("Aplikace ($packageName) není nainstalována."),
                )
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            app.startActivity(launch)
            Result.success("Otevírám $label.")
        } catch (_: ActivityNotFoundException) {
            Result.failure(IllegalStateException("Nelze spustit aplikaci ($packageName)."))
        }
    }
}
