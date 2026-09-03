package cz.handy.feature.actions.phone

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.text.Normalizer
import kotlin.math.max
import kotlin.math.min

data class ResolvedContact(
    val displayName: String,
    val phoneNumber: String,
    val telUri: Uri,
)

object DeviceContactFuzzyResolver {

    fun findClosestContact(context: Context, spokenName: String): ResolvedContact? {
        val q = spokenName.trim()
        if (q.isEmpty()) return null

        DialableNumberComposer.tryTelUriDigitsOnly(q)?.let {
            return ResolvedContact(
                displayName = q,
                phoneNumber = it.schemeSpecificPart,
                telUri = it,
            )
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val allContacts = loadPhoneContacts(context)
        if (allContacts.isEmpty()) return null

        val normQ = normalize(q)
        var bestContact: ResolvedContact? = null
        var bestScore = 0.0

        for (contact in allContacts) {
            val score = scoreMatch(normQ, normalize(contact.displayName))
            if (score > bestScore) {
                bestScore = score
                bestContact = contact
            }
        }

        return if (bestScore >= 0.35) {
            bestContact
        } else {
            allContacts.firstOrNull {
                val target = normalize(it.displayName)
                target.contains(normQ) || normQ.contains(target)
            }
        }
    }

    private fun loadPhoneContacts(context: Context): List<ResolvedContact> {
        val list = ArrayList<ResolvedContact>()
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"

        val cursor = runCatching {
            context.contentResolver.query(uri, projection, null, null, sortOrder)
        }.getOrNull() ?: return emptyList()

        cursor.use { c ->
            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext()) {
                val name = if (nameIdx >= 0) c.getString(nameIdx) else null
                val num = if (numIdx >= 0) c.getString(numIdx) else null
                if (!name.isNullOrBlank() && !num.isNullOrBlank()) {
                    val cleanNum = num.replace(Regex("[^0-9+]"), "")
                    if (cleanNum.isNotEmpty()) {
                        list.add(
                            ResolvedContact(
                                displayName = name.trim(),
                                phoneNumber = cleanNum,
                                telUri = Uri.parse("tel:$cleanNum"),
                            ),
                        )
                    }
                }
            }
        }
        return list
    }

    internal fun normalize(s: String): String {
        val nfd = Normalizer.normalize(s, Normalizer.Form.NFD)
        return nfd.replace(Regex("\\p{M}"), "")
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    internal fun scoreMatch(query: String, target: String): Double {
        if (query.isEmpty() || target.isEmpty()) return 0.0
        if (query == target) return 1.0

        val targetWords = target.split(" ").filter { it.isNotEmpty() }
        val queryWords = query.split(" ").filter { it.isNotEmpty() }

        if (targetWords.contains(query)) return 0.95
        if (queryWords.all { targetWords.contains(it) }) return 0.92
        if (targetWords.any { it.startsWith(query) || query.startsWith(it) }) return 0.85
        if (target.contains(query)) return 0.80

        var maxWordSim = 0.0
        for (tw in targetWords) {
            for (qw in queryWords) {
                val sim = wordSimilarity(qw, tw)
                if (sim > maxWordSim) maxWordSim = sim
            }
        }
        return maxWordSim
    }

    private fun wordSimilarity(w1: String, w2: String): Double {
        val maxLen = max(w1.length, w2.length)
        if (maxLen == 0) return 1.0
        val dist = levenshtein(w1, w2)
        return (maxLen - dist).toDouble() / maxLen
    }

    private fun levenshtein(s1: String, s2: String): Int {
        val dp = IntArray(s2.length + 1) { it }
        for (i in 1..s1.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..s2.length) {
                val temp = dp[j]
                dp[j] = if (s1[i - 1] == s2[j - 1]) prev else min(prev, min(dp[j], dp[j - 1])) + 1
                prev = temp
            }
        }
        return dp[s2.length]
    }
}
