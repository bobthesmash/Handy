package cz.handy.feature.asr

/**
 * PCM after the English `navigate to` prefix. [prefixEndSample] is preferred;
 * otherwise the cut is estimated from English word counts.
 */
object NavigatePlaceTailPcm {
    fun slice(
        utterance: ShortArray,
        prefixEndSample: Int?,
        prefixWordCount: Int,
        englishWordCount: Int,
    ): ShortArray {
        if (utterance.isEmpty()) return utterance
        val cut = cutIndex(utterance.size, prefixEndSample, prefixWordCount, englishWordCount)
        if (cut <= 0) return utterance
        if (cut >= utterance.size) return ShortArray(0)
        return utterance.copyOfRange(cut, utterance.size)
    }

    fun slicedTailOnly(
        utterance: ShortArray,
        prefixEndSample: Int?,
    ): Boolean {
        val end = prefixEndSample ?: return false
        return end in 1 until utterance.size
    }

    private fun cutIndex(
        utteranceSize: Int,
        prefixEndSample: Int?,
        prefixWordCount: Int,
        englishWordCount: Int,
    ): Int {
        if (prefixEndSample != null && prefixEndSample in 1 until utteranceSize) {
            return prefixEndSample
        }
        if (prefixWordCount > 0 && englishWordCount > prefixWordCount) {
            return ((prefixWordCount.toLong() * utteranceSize) / englishWordCount)
                .toInt()
                .coerceIn(0, utteranceSize - 1)
        }
        return 0
    }
}
