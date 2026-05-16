package cz.handy.feature.actions.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import cz.handy.core.persistence.ContactAliasStore

/** Společná logika pro slot **`contact`** u hovoru i SMS ([F1-T09], [F1-T10]). */
internal object PhoneSlotResolver {
    fun resolveTelUri(
        context: Context,
        contactSlotRaw: String,
    ): Uri? {
        val app = context.applicationContext
        val expanded = ContactAliasStore(app).expandForDial(contactSlotRaw)
        DialableNumberComposer.tryTelUriDigitsOnly(expanded)?.let {
            return it
        }
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return ContactDialUriLookup.tryTelUri(app.contentResolver, expanded)
    }

    /** Adresát pro SMS / `SmsManager` z normalizovaného `tel:` [Uri] (včetně úvodního `+`). */
    fun resolveSmsDestinationAddress(
        context: Context,
        contactSlotRaw: String,
    ): String? = resolveTelUri(context, contactSlotRaw)?.schemeSpecificPart?.takeIf { it.isNotBlank() }
}
