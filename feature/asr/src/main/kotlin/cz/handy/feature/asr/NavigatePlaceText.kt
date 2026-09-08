package cz.handy.feature.asr

/**
 * Turns a Czech Vosk transcript into a Maps `{place}` string.
 * When the decoder saw only the tail PCM, the text is the place as-is.
 */
object NavigatePlaceText {
    fun placeFromCzechDecode(
        czechText: String,
        decodedTailOnly: Boolean,
        englishPrefixWordCount: Int,
    ): String {
        val trimmed = czechText.trim()
        if (trimmed.isEmpty()) return ""
        if (decodedTailOnly) return trimmed
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (englishPrefixWordCount > 0 && words.size > englishPrefixWordCount) {
            return words.drop(englishPrefixWordCount).joinToString(" ")
        }
        return trimmed
    }
}
