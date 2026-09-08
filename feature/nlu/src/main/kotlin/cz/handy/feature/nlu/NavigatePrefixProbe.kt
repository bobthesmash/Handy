package cz.handy.feature.nlu

/**
 * Detects an English (or catalog) `NAVIGATE` command prefix on streaming partials
 * so the remaining PCM can be decoded with a location-selected place ASR.
 */
data class NavigatePrefixHit(
    val prefix: String,
    val wordCount: Int,
    val placeAlreadyStarted: Boolean,
)

object NavigatePrefixProbe {
    fun prefixes(): List<String> = prefixList

    fun match(transcript: String): NavigatePrefixHit? {
        val n = AsrUtteranceNormalizer.normalize(transcript)
        val words = n.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        var best: NavigatePrefixHit? = null
        for (prefix in prefixListByLongest) {
            val pw = prefix.split(' ').filter { it.isNotEmpty() }
            if (pw.isEmpty() || words.size < pw.size) continue
            val head = words.take(pw.size).joinToString(" ")
            if (head != prefix && !PhraseSpellingTolerance.within(head, prefix)) continue
            val hit =
                NavigatePrefixHit(
                    prefix = prefix,
                    wordCount = pw.size,
                    placeAlreadyStarted = words.size > pw.size,
                )
            if (best == null || hit.wordCount > best.wordCount) {
                best = hit
            }
        }
        return best
    }

    private val prefixList: List<String> by lazy { computePrefixes() }

    private val prefixListByLongest: List<String> by lazy {
        prefixList.sortedByDescending { it.split(' ').size }
    }

    private fun computePrefixes(): List<String> {
        val catalogs = listOf(HandyNluCatalogs.enMinimal, HandyNluCatalogs.mvp)
        return catalogs
            .flatMap { catalog ->
                catalog.intents
                    .filter { it.id == "NAVIGATE" }
                    .flatMap { def ->
                        def.matchers
                            .filter { it.orderedSlotNames == listOf("place") }
                            .map { it.leadingLiteral.trim() }
                    }
            }.filter { it.isNotEmpty() }
            .distinct()
    }
}
