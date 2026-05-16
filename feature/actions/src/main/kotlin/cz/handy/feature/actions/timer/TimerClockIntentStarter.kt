package cz.handy.feature.actions.timer

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock

/**
 * Otevře obrazovku časovače s předvyplněnou délkou ([F2-T08]).
 */
object TimerClockIntentStarter {
    fun tryStartTimer(
        context: Context,
        lengthSeconds: Int,
    ): Boolean {
        val sec = lengthSeconds.coerceIn(1, CzechDurationParser.MAX_SECONDS)
        val intent =
            Intent(AlarmClock.ACTION_SET_TIMER).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra(AlarmClock.EXTRA_LENGTH, sec)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            }
        return runCatching { context.applicationContext.startActivity(intent) }.isSuccess
    }
}
