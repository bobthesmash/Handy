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
    private val CS = Locale.forLanguageTag("cs-CZ")

    fun timeSentence(clock: Clock = Clock.systemDefaultZone()): String {
        val z = ZonedDateTime.now(clock)
        val fmt = DateTimeFormatter.ofPattern("H:mm", CS)
        return "Je ${z.format(fmt)}."
    }

    fun dateSentence(
        zone: ZoneId = ZoneId.systemDefault(),
        clock: Clock = Clock.system(zone),
    ): String {
        val z = ZonedDateTime.now(clock)
        val fmt = DateTimeFormatter.ofPattern("EEEE d. MMMM yyyy", CS)
        return "Je ${z.format(fmt)}."
    }

    fun batterySentence(context: Context): Result<String> {
        val pct =
            readBatteryPercent(context) ?: return Result.failure(
                IllegalStateException("Nepodařilo se přečíst stav baterie."),
            )
        return Result.success("Baterie je na $pct procentech.")
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
