package cz.handy.feature.actions.alarm

/**
 * Výklad slotu **`time`** pro budík (ne úplný CZ NLP — jen běžné digitální formáty).
 */
internal object AlarmSlotTimeParser {
    private const val LAST_HOUR_OF_DAY = 23
    private const val LAST_MINUTE_OF_HOUR = 59

    /**
     * @return `(hodina,minuta)` pokud rozparsováno jinak `null`.
     */
    fun parseHourMinute(raw: String): Pair<Int, Int>? {
        val t =
            raw
                .trim()
                .lowercase()
                .replace(",", ":")
                .replace(Regex("\\s+"), "")
                .ifBlank { return null }

        fun twoPartsDigit(
            hi: Int,
            mi: Int,
        ): Pair<Int, Int>? =
            when {
                hi in 0..LAST_HOUR_OF_DAY && mi in 0..LAST_MINUTE_OF_HOUR -> Pair(hi, mi)
                else -> null
            }

        if (Regex("""^\d+$""").matches(t)) {
            return when (t.length) {
                4 -> {
                    val hh = t.substring(0, 2).toInt()
                    val mm = t.substring(2, 4).toInt()
                    twoPartsDigit(hh, mm)
                }
                3 -> {
                    val hh = t.substring(0, 1).toInt()
                    val mm = t.substring(1, 3).toInt()
                    twoPartsDigit(hh, mm)
                }
                2 -> twoPartsDigit(t.toInt(), 0)
                1 -> twoPartsDigit(t.toInt(), 0)
                else -> null
            }
        }

        val colonParts =
            raw
                .trim()
                .replace(",", ":")
                .split(":")
                .filter { it.isNotBlank() }
                .mapNotNull { seg -> seg.trim().toIntOrNull() }
        if (colonParts.size >= 2) {
            val hi = colonParts[0].coerceIn(0, LAST_HOUR_OF_DAY)
            val mi = colonParts[1].coerceIn(0, LAST_MINUTE_OF_HOUR)
            return twoPartsDigit(hi, mi)
        }

        val spaceParts =
            Regex("""(\d+)\s*[.:]\s*(\d+)""")
                .find(raw)
                ?.groupValues
                ?.drop(1)
                ?.map(String::toInt)
        return if (spaceParts != null && spaceParts.size == 2) {
            twoPartsDigit(spaceParts[0], spaceParts[1])
        } else {
            null
        }
    }
}
