package cz.handy.feature.actions.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

/**
 * Zahájí GSM/VoLTE hovor přes [TelecomManager.placeCall].
 *
 * [contactSlotRaw] vzniká z NLU slotu **`contact`** — číslo (včetně `+`/oddělovačů)
 * nebo řetězec pro filtrování kontaktu ([F1-T09]).
 */
class TelecomCallPlacer(
    context: Context,
) {
    private val appCtx = context.applicationContext
    private val telecom =
        appCtx.getSystemService(Context.TELECOM_SERVICE) as TelecomManager

    /**
     * @return [Result.failure] při chybějícím **`CALL_PHONE`**, rozparsovatelném čísle/kontaktu,
     * nebo při výjimce z Telephony (`SecurityException`).
     *
     * Pro kontaktní řetězce je potřeba **`READ_CONTACTS`** — bez něj se zkusí jen číselný formát slotu.
     */
    @RequiresPermission(Manifest.permission.CALL_PHONE)
    fun placeCallUri(uri: Uri): Result<Unit> {
        if (!hasCallPermission()) {
            return Result.failure(
                SecurityException("${Manifest.permission.CALL_PHONE}: permission not granted."),
            )
        }
        return runCatching {
            telecom.placeCall(uri, Bundle.EMPTY)
            Unit
        }
    }

    @RequiresPermission(Manifest.permission.CALL_PHONE)
    fun placeOutgoingCall(contactSlotRaw: String): Result<Unit> {
        if (!hasCallPermission()) {
            return Result.failure(
                SecurityException("${Manifest.permission.CALL_PHONE}: permission not granted."),
            )
        }

        val uri =
            PhoneSlotResolver.resolveTelUri(appCtx, contactSlotRaw)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "Nelze rozřešit číslo nebo kontakt: $contactSlotRaw",
                    ),
                )

        return placeCallUri(uri)
    }

    private fun hasCallPermission(): Boolean =
        ContextCompat.checkSelfPermission(appCtx, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
}
