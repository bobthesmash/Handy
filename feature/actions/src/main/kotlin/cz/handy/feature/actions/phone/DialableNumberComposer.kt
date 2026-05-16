package cz.handy.feature.actions.phone

import android.net.Uri

/**
 * Rychlé `tel:` z slotu jen z číslicích (+ volitelný úvodní `+`).
 * Jakmile ve slotu poznáme **písmeno**, vrátí se `null` a volá se název přes kontakty.
 */
internal object DialableNumberComposer {
    private val letter = Regex("\\p{L}")
    private const val MIN_DIGITS = 7
    private const val MAX_DIGITS = 15

    fun tryTelUriDigitsOnly(slot: String): Uri? {
        val spec = tryTelSpec(slot) ?: return null
        /** [Uri.encode] zachová + na začátku špatně jen při prázdném hostu — pro `tel` stačí parse. */
        return Uri.fromParts("tel", spec, null)
    }

    /** Čistě řetězcový výsledek pro JVM unit testy (bez [Uri]). */
    internal fun tryTelSpec(slot: String): String? {
        val t = slot.trim().ifBlank { return null }
        if (letter.containsMatchIn(t)) return null
        return buildTelecomSpecFromSanitizedChars(t)
    }

    private fun buildTelecomSpecFromSanitizedChars(trimmedSlot: CharSequence): String? {
        val sb = StringBuilder()
        var rejected = false
        for (ch in trimmedSlot) {
            when {
                rejected -> break
                ch in '0'..'9' -> sb.append(ch)
                ch == '+' -> {
                    if (sb.isNotEmpty()) {
                        rejected = true
                    } else {
                        sb.append('+')
                    }
                }
                isDialFormattingSeparator(ch) -> Unit
                else -> rejected = true
            }
        }
        if (rejected || sb.isEmpty()) return null
        val digitCount = sb.count { it.isDigit() }
        if (digitCount !in MIN_DIGITS..MAX_DIGITS) return null

        val out = sb.toString()
        if ('+' in out && !out.startsWith('+')) return null
        /** `+` se toleruje jen úplně najednou na začátku řetězce výše. */
        if (countChar(out, '+') > 1) return null
        return out
    }

    private fun isDialFormattingSeparator(ch: Char): Boolean =
        ch == ' ' ||
            ch == '\u00a0' ||
            ch == '-' ||
            ch == '(' ||
            ch == ')' ||
            ch == '/' ||
            ch == '.' ||
            ch == ','

    private fun countChar(
        s: String,
        ch: Char,
    ): Int = s.count { it == ch }
}
