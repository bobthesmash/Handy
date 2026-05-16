package cz.handy.feature.actions.alarm

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

/**
 * Předvyplnění aplikace budíku systémovým [`AlarmClock.ACTION_SET_ALARM`] ([F1-T11]).
 */
object AlarmClockIntentStarter {
    fun tryStartAlarm(
        context: Context,
        hour: Int,
        minutes: Int,
        message: String?,
    ): Boolean {
        val hi = hour.coerceIn(0, 23)
        val mi = minutes.coerceIn(0, 59)
        val intent =
            Intent(AlarmClock.ACTION_SET_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AlarmClock.EXTRA_HOUR, hi)
                putExtra(AlarmClock.EXTRA_MINUTES, mi)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                if (!message.isNullOrBlank()) {
                    putExtra(AlarmClock.EXTRA_MESSAGE, message.trim())
                }
            }
        return runCatching { context.applicationContext.startActivity(intent) }.isSuccess
    }
}
