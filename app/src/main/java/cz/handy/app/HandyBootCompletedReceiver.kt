package cz.handy.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import cz.handy.core.audio.EarService

/**
 * Restores [EarService] after reboot so assistive listening can resume ([F0-T09]).
 * OEM battery optimisations may still stop the foreground service later.
 */
class HandyBootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent?,
    ) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        EarService.start(context.applicationContext)
    }
}
