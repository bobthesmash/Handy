package cz.handy.feature.actions.info

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.time.Clock
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Odpovědi pro info intenty bez systémové akce ([F2-T07]) — čas, datum, stav baterie.
 */
object DeviceInfoAnswers {
    private val EN = Locale.US

    fun timeSentence(clock: Clock = Clock.systemDefaultZone()): String {
        val z = ZonedDateTime.now(clock)
        val fmt = DateTimeFormatter.ofPattern("h:mm a", EN)
        return "It is ${z.format(fmt)}."
    }

    fun dateSentence(
        zone: ZoneId = ZoneId.systemDefault(),
        clock: Clock = Clock.system(zone),
    ): String {
        val z = ZonedDateTime.now(clock)
        val fmt = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", EN)
        return "It is ${z.format(fmt)}."
    }

    fun batterySentence(context: Context): Result<String> {
        val pct =
            readBatteryPercent(context) ?: return Result.failure(
                IllegalStateException("Could not read battery level."),
            )
        return Result.success("Battery is at $pct percent.")
    }

    private fun readBatteryPercent(context: Context): Int? {
        val app = context.applicationContext
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val status = app.registerReceiver(null, filter) ?: return null
        val level = status.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = status.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level * 100f / scale).toInt().coerceIn(0, 100)
    }
}
