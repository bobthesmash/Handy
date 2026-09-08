package cz.handy.feature.actions.nav

import android.app.Activity
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager

/**
 * Turns the screen on, dismisses keyguard when allowed, then starts Maps navigation.
 * Locked-phone friendly trampoline so NAVIGATE can leave Handy from the pocket/bike.
 */
class LockscreenTrampolineActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockscreen()
        val primary = intent.getStringExtra(EXTRA_PRIMARY_URI).orEmpty()
        val fallback = intent.getStringExtra(EXTRA_FALLBACK_URI).orEmpty()
        val pkg = intent.getStringExtra(EXTRA_PACKAGE)
        if (primary.isBlank() && fallback.isBlank()) {
            Log.e(TAG, "missing navigation URI extras")
            finish()
            return
        }
        dismissThen {
            val started =
                startTurnByTurn(primary, pkg) ||
                    startTurnByTurn(primary, packageName = null) ||
                    startTurnByTurn(fallback, pkg) ||
                    startTurnByTurn(fallback, packageName = null)
            if (!started) {
                Log.e(TAG, "no handler for navigation URIs primary=$primary fallback=$fallback")
            }
            finish()
        }
    }

    private fun showOverLockscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD,
            )
        }
    }

    private fun dismissThen(block: () -> Unit) {
        val km = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && km != null && km.isKeyguardLocked) {
            km.requestDismissKeyguard(
                this,
                object : KeyguardManager.KeyguardDismissCallback() {
                    override fun onDismissSucceeded() = block()

                    override fun onDismissCancelled() = block()

                    override fun onDismissError() = block()
                },
            )
        } else {
            block()
        }
    }

    private fun startTurnByTurn(
        uriString: String,
        packageName: String?,
    ): Boolean {
        if (uriString.isBlank()) return false
        if (MapsNavigationUris.isPreviewOnly(uriString)) {
            Log.w(TAG, "refusing preview-only URI $uriString")
            return false
        }
        val view =
            Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (!packageName.isNullOrBlank()) {
                    setPackage(packageName)
                }
            }
        return try {
            startActivity(view)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    companion object {
        const val EXTRA_PRIMARY_URI = "cz.handy.nav.PRIMARY_URI"
        const val EXTRA_FALLBACK_URI = "cz.handy.nav.FALLBACK_URI"
        const val EXTRA_PACKAGE = "cz.handy.nav.PACKAGE"
        private const val TAG = "HandyMapsNav"
    }
}
