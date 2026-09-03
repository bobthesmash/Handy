package cz.handy.feature.actions.media

/**
 * Mapuje volný text z NLU slotu [command] na [MediaTransportCommand] ([F2-T04]).
 */
object MediaCtrlCommandParser {
    fun parse(raw: String): MediaTransportCommand? {
        val c = raw.trim().lowercase()
        if (c.isBlank()) return null
        return when {
            c.contains("předchoz") ||
                c.contains("předešl") ||
                c.contains("zpátky") ||
                c.contains("zpět") ||
                c.contains("go back") ||
                c.contains("previous") -> MediaTransportCommand.Previous

            c.contains("další") ||
                c.contains("přeskak") ||
                c.contains("skip") ||
                c == "next" -> MediaTransportCommand.Next

            c.contains("pauza") ||
                c.contains("stop") ||
                c.contains("zastav") -> MediaTransportCommand.Pause

            c.contains("pokračuj") ||
                c.contains("resume") ||
                c == "play" -> MediaTransportCommand.Play

            else -> null
        }
    }
}
