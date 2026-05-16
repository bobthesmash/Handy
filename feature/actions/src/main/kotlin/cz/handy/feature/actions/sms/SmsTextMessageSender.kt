package cz.handy.feature.actions.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import cz.handy.feature.actions.phone.PhoneSlotResolver

/**
 * Odešle SMS přes [SmsManager] po rozřešení NLU slotů **`contact`** a **`message`**.
 *
 * Volající musí předat **`confirmedByUser = true`**, jinak viz [requireDestructiveSmsUserConfirmation].
 */
class SmsTextMessageSender(
    context: Context,
) {
    private val appCtx = context.applicationContext

    /**
     * @return [Result.failure] při chybějícím **`SEND_SMS`**, `confirmedByUser == false`,
     * rozparsovatelném čísle/kontaktu, prázdné zprávě, nebo při výjimce z rádiové vrstvy.
     */
    @RequiresPermission(Manifest.permission.SEND_SMS)
    fun sendTextMessage(
        confirmedByUser: Boolean,
        contactSlotRaw: String,
        messageBody: String,
    ): Result<Unit> {
        requireDestructiveSmsUserConfirmation(confirmedByUser).getOrElse { return Result.failure(it) }

        if (!hasSendSmsPermission()) {
            return Result.failure(
                SecurityException("${Manifest.permission.SEND_SMS}: permission not granted."),
            )
        }

        val destination =
            PhoneSlotResolver.resolveSmsDestinationAddress(appCtx, contactSlotRaw)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Nelze rozřešit číslo nebo kontakt: $contactSlotRaw",
                    ),
                )

        val trimmed = messageBody.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Prázdná textová zpráva SMS."))
        }

        val smsMgr = smsManagerCompat()
        return runCatching {
            val parts = smsMgr.divideMessage(trimmed)
            if (parts.size <= 1) {
                smsMgr.sendTextMessage(destination, null, trimmed, null, null)
            } else {
                @Suppress("DEPRECATION")
                smsMgr.sendMultipartTextMessage(destination, null, parts, null, null)
            }
            Unit
        }
    }

    private fun hasSendSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(appCtx, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun smsManagerCompat(): SmsManager =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            checkNotNull(appCtx.getSystemService(SmsManager::class.java))
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}
