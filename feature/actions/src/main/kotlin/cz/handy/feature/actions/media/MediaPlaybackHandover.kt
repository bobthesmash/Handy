package cz.handy.feature.actions.media

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import cz.handy.feature.actions.notification.HandyNotificationListenerService

/**
 * Transportní akce nad aktivní relací ([F2-T04]).
 */
enum class MediaTransportCommand {
    Next,
    Previous,
    Pause,
    Play,
}

/**
 * Spouští přehrávání přes aktivní [MediaController] relace (vyžaduje povolený NLS Handy)
 * nebo předá řízení aplikaci Spotify / YouTube Music přes launcher intent ([F2-T03]).
 */
class MediaPlaybackHandover(
    context: Context,
) {
    private val app = context.applicationContext

    fun transport(cmd: MediaTransportCommand): Result<String> {
        val ctrls =
            activeControllers()
                ?: return Result.failure(mediaControlUnavailable())
        if (ctrls.isEmpty()) {
            return Result.failure(IllegalStateException("Žádná aktivní mediální relace."))
        }
        val target = pickPreferredController(ctrls)
        val tc = target.transportControls
        when (cmd) {
            MediaTransportCommand.Next -> tc.skipToNext()
            MediaTransportCommand.Previous -> tc.skipToPrevious()
            MediaTransportCommand.Pause -> tc.pause()
            MediaTransportCommand.Play -> tc.play()
        }
        return Result.success(transportAck(cmd))
    }

    @Suppress("ReturnCount")
    fun play(appSlot: String?): Result<String> {
        val pkg = resolveTargetPackage(appSlot)
        if (pkg != null) {
            val targeted = tryPlayPackage(pkg)
            if (targeted.isSuccess) return targeted
            return launchPackage(pkg)
        }
        val any = tryPlayAnySession()
        if (any.isSuccess) return any
        return launchDefaultMusicChooser()
    }

    private fun resolveTargetPackage(appSlot: String?): String? {
        val s = appSlot?.trim()?.lowercase().orEmpty()
        if (s.isBlank()) return null
        return when {
            s.contains("spotify") -> "com.spotify.music"
            s.contains("youtube") ||
                s.contains("youtub") ||
                s.contains("yt music") ||
                s.contains("ytm") ||
                s == "yt" -> "com.google.android.apps.youtube.music"
            s.contains("apple") && s.contains("music") -> "com.apple.android.music"
            else -> null
        }
    }

    private fun tryPlayPackage(pkg: String): Result<String> {
        val ctrls =
            activeControllers()
                ?: return Result.failure(mediaControlUnavailable())
        val ours =
            ctrls.filter { it.packageName == pkg }
        if (ours.isEmpty()) {
            return Result.failure(IllegalStateException("Aktivní relace pro $pkg není k dispozici."))
        }
        return trySendPlay(ours, label = friendlyAppLabel(pkg))
    }

    private fun tryPlayAnySession(): Result<String> {
        val ctrls =
            activeControllers()
                ?: return Result.failure(mediaControlUnavailable())
        if (ctrls.isEmpty()) {
            return Result.failure(IllegalStateException("Žádná aktivní mediální relace."))
        }
        var anyPlaying = false
        for (c in ctrls) {
            when (c.playbackState?.state) {
                PlaybackState.STATE_PLAYING,
                PlaybackState.STATE_BUFFERING,
                -> anyPlaying = true
                else -> Unit
            }
        }
        if (anyPlaying) {
            return Result.success("Média už přehrávají.")
        }
        return trySendPlay(ctrls, label = "přehrávání")
    }

    private fun trySendPlay(
        controllers: List<MediaController>,
        label: String,
    ): Result<String> {
        for (c in controllers) {
            c.transportControls.play()
            return Result.success("Spouštím $label.")
        }
        return Result.failure(IllegalStateException("Nelze spustit transportní ovládání."))
    }

    private fun pickPreferredController(ctrls: List<MediaController>): MediaController {
        for (c in ctrls) {
            when (c.playbackState?.state) {
                PlaybackState.STATE_PLAYING,
                PlaybackState.STATE_BUFFERING,
                -> return c
                else -> Unit
            }
        }
        return ctrls.first()
    }

    private fun transportAck(cmd: MediaTransportCommand): String =
        when (cmd) {
            MediaTransportCommand.Next -> "Další skladba."
            MediaTransportCommand.Previous -> "Předchozí skladba."
            MediaTransportCommand.Pause -> "Pauza."
            MediaTransportCommand.Play -> "Přehrávání pokračuje."
        }

    private fun friendlyAppLabel(pkg: String): String =
        when (pkg) {
            "com.spotify.music" -> "Spotify"
            "com.google.android.apps.youtube.music" -> "YouTube Music"
            else -> pkg
        }

    private fun launchPackage(pkg: String): Result<String> {
        val launch =
            app.packageManager.getLaunchIntentForPackage(pkg)
                ?: return Result.failure(
                    IllegalStateException("Aplikace není nainstalována ($pkg)."),
                )
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            app.startActivity(launch)
            Result.success("Otevírám ${friendlyAppLabel(pkg)}.")
        } catch (_: ActivityNotFoundException) {
            Result.failure(IllegalStateException("Nelze otevřít aplikaci ($pkg)."))
        }
    }

    private fun launchDefaultMusicChooser(): Result<String> {
        val intent =
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MUSIC)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        return try {
            app.startActivity(intent)
            Result.success("Otevírám hudbu.")
        } catch (_: ActivityNotFoundException) {
            Result.failure(
                IllegalStateException("Není dostupná výchozí hudební aplikace."),
            )
        }
    }

    private fun activeControllers(): List<MediaController>? {
        val msm = app.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val cn = ComponentName(app, HandyNotificationListenerService::class.java)
        return try {
            msm.getActiveSessions(cn)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun mediaControlUnavailable(): IllegalStateException =
        IllegalStateException(
            "Povolte Handy „přístup k oznámením“ — bez něj systém nesdílí aktivní mediální relace.",
        )
}
