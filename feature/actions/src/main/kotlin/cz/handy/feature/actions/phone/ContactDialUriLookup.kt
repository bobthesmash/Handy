package cz.handy.feature.actions.phone

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract

internal object ContactDialUriLookup {
    /** První nalezené číslo pro filtrovaný výraz (jméno / alias přes Contacts UI). */
    fun tryTelUri(
        cr: ContentResolver,
        displayNameGuess: String,
    ): Uri? {
        val q = displayNameGuess.trim().ifBlank { return null }

        val contactUri =
            ContactsContract.Contacts.CONTENT_FILTER_URI
                .buildUpon()
                .appendPath(q)
                .build()

        val cursor =
            cr.query(
                contactUri,
                arrayOf(ContactsContract.Contacts._ID),
                null,
                null,
                null,
            )
                ?: return null

        return cursor.use { probe ->
            if (!probe.moveToFirst()) {
                null
            } else {
                val idIdx = probe.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val contactId = probe.getLong(idIdx)
                findFirstPhone(cr, contactId)
            }
        }
    }

    private fun findFirstPhone(
        cr: ContentResolver,
        contactId: Long,
    ): Uri? {
        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID}=?"
        val args = arrayOf(contactId.toString())

        /**
         * [IS_PRIMARY] — preferovat uložená primární čísla nad dalšími řádky kontaktu.
         */
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"

        val cursor =
            cr.query(
                phoneUri,
                projection,
                selection,
                args,
                sortOrder,
            )
                ?: return null

        return cursor.use { pc ->
            if (!pc.moveToFirst()) {
                null
            } else {
                val nIdx = pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (pc.isNull(nIdx)) {
                    null
                } else {
                    DialableNumberComposer.tryTelUriDigitsOnly(
                        pc.getString(nIdx) ?: "",
                    )
                }
            }
        }
    }
}
