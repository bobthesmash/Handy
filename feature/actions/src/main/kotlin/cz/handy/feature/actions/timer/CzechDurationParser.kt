package cz.handy.feature.actions.timer

import java.util.Locale

/**
 * Parsuje krátké české zápisy trvání do sekund ([F2-T08]).
 */
object CzechDurationParser {
    private val CS = Locale.forLanguageTag("cs-CZ")

    private const val SECONDS_PER_MINUTE = 60
    private const val SECONDS_PER_HOUR = 3600
    private const val HALF_HOUR_SECONDS = 30 * SECONDS_PER_MINUTE
    private const val QUARTER_HOUR_SECONDS = 15 * SECONDS_PER_MINUTE
    private const val WORD_ELEVEN = 11
    private const val WORD_TWELVE = 12
    private const val WORD_FIFTEEN = 15
    private const val WORD_TWENTY = 20
    private const val WORD_THIRTY = 30

    private val WORD_MINUTES =
        mapOf(
            "jednu" to 1,
            "jedna" to 1,
            "jednom" to 1,
            "dvě" to 2,
            "dva" to 2,
            "tři" to 3,
            "čtyři" to 4,
            "pět" to 5,
            "šest" to 6,
            "sedm" to 7,
            "osm" to 8,
            "devět" to 9,
            "deset" to 10,
            "jedenáct" to WORD_ELEVEN,
            "dvanáct" to WORD_TWELVE,
            "patnáct" to WORD_FIFTEEN,
            "dvacet" to WORD_TWENTY,
            "třicet" to WORD_THIRTY,
        )

    private val RX_MINUTES = Regex("""(\d+)\s*minut""")
    private val RX_HOURS = Regex("""(\d+)\s*hodin""")
    private val RX_SECONDS = Regex("""(\d+)\s*sekund""")
    private val RX_MIN_SHORT = Regex("""(\d+)\s*min\b""")
    private val RX_H_SHORT = Regex("""(\d+)\s*hod\b""")

    /** Maximální povolená délka časovače (12 hodin). */
    const val MAX_SECONDS = 12 * SECONDS_PER_HOUR

    fun parseToSeconds(raw: String): Int? {
        val s = raw.trim().lowercase(CS).replace(Regex("\\s+"), " ")
        if (s.isBlank()) return null

        parseHalfOrQuarterHourPhrases(s)
            ?.let { return it }

        parseNumericUnitMatches(s)
            ?.let { return it }

        return parseWordMinuteForms(s)
    }

    private fun parseHalfOrQuarterHourPhrases(s: String): Int? =
        when {
            "půl hodiny" in s || "pul hodiny" in s || "půlhodin" in s -> HALF_HOUR_SECONDS
            ("čtvrt" in s && "hodin" in s) || "čtvrthodin" in s -> QUARTER_HOUR_SECONDS
            else -> null
        }

    private fun parseNumericUnitMatches(s: String): Int? =
        RX_MINUTES.matchEntire(s)?.groupValues?.get(1)?.toIntOrNull()?.let {
            (it * SECONDS_PER_MINUTE).coerceIn(1, MAX_SECONDS)
        }
            ?: RX_HOURS.find(s)?.groupValues?.get(1)?.toIntOrNull()?.let {
                (it * SECONDS_PER_HOUR).coerceIn(1, MAX_SECONDS)
            }
            ?: RX_SECONDS.find(s)?.groupValues?.get(1)?.toIntOrNull()?.let {
                it.coerceIn(1, MAX_SECONDS)
            }
            ?: RX_MIN_SHORT.find(s)?.groupValues?.get(1)?.toIntOrNull()?.let {
                (it * SECONDS_PER_MINUTE).coerceIn(1, MAX_SECONDS)
            }
            ?: RX_H_SHORT.find(s)?.groupValues?.get(1)?.toIntOrNull()?.let {
                (it * SECONDS_PER_HOUR).coerceIn(1, MAX_SECONDS)
            }

    private fun parseWordMinuteForms(s: String): Int? {
        val minTail = Regex("""^(.+)\s+minut$""").find(s) ?: Regex("""^(.+)\s+minutu$""").find(s)
        val minutesFromTail =
            minTail
                ?.groupValues
                ?.get(1)
                ?.trim()
                ?.let { WORD_MINUTES[it] }
        val minutes = minutesFromTail ?: WORD_MINUTES[s]
        return minutes?.let { (it * SECONDS_PER_MINUTE).coerceIn(1, MAX_SECONDS) }
    }
}
