package cz.handy.core.common.audio

/**
 * Speech-onset media duck: pause playback for [WINDOW_MS] so ASR can hear,
 * then resume unless the command was pause/stop.
 *
 * Music / lyric hallucinations produce rapid, low-value ASR partials. Those must
 * not pause playback; a real command onset still must.
 */
class SpeechOnsetDuckPolicy {
    enum class Action {
        None,
        PauseNow,
        ResumeNow,
        KeepPaused,
    }

    data class Event(
        val action: Action,
        val generation: Int,
    )

    data class OnsetSample(
        val text: String,
        val minTokenProb: Float?,
        val playbackActive: Boolean,
        val listeningForCommand: Boolean,
        val nowMs: Long,
    )

    var isDucked: Boolean = false
        private set

    private var generation: Int = 0
    private val recent = ArrayDeque<RecentHypothesis>()

    fun onSpeechHeard(sample: OnsetSample): Event {
        if (isDucked || !sample.playbackActive || !sample.listeningForCommand) {
            return Event(Action.None, generation)
        }
        val normalized = normalize(sample.text)
        if (!passesQuality(normalized, sample.minTokenProb)) {
            remember(normalized, sample.nowMs)
            return Event(Action.None, generation)
        }
        prune(sample.nowMs)
        if (isMusicChatter(normalized, sample.nowMs)) {
            remember(normalized, sample.nowMs)
            return Event(Action.None, generation)
        }
        if (!shouldDuckNow(normalized, sample.nowMs)) {
            remember(normalized, sample.nowMs)
            return Event(Action.None, generation)
        }
        remember(normalized, sample.nowMs)
        isDucked = true
        generation += 1
        return Event(Action.PauseNow, generation)
    }

    fun onCommandResolved(keepPlaybackPaused: Boolean): Event {
        if (!isDucked) {
            return Event(Action.None, generation)
        }
        isDucked = false
        return if (keepPlaybackPaused) {
            Event(Action.KeepPaused, generation)
        } else {
            Event(Action.ResumeNow, generation)
        }
    }

    fun onWindowElapsed(expectedGeneration: Int): Event {
        if (expectedGeneration != generation || !isDucked) {
            return Event(Action.None, generation)
        }
        isDucked = false
        return Event(Action.ResumeNow, generation)
    }

    private fun passesQuality(
        normalized: String,
        minTokenProb: Float?,
    ): Boolean {
        if (normalized.length < MIN_TEXT_CHARS) return false
        if (normalized.count { it.isLetter() } < MIN_LETTER_CHARS) return false
        if (minTokenProb != null && minTokenProb < MIN_DUCK_TOKEN_PROB) return false
        return true
    }

    private fun shouldDuckNow(
        normalized: String,
        nowMs: Long,
    ): Boolean {
        if (looksLikeImmediateCommand(normalized)) return true
        val previous = recent.lastOrNull { isSameUtterance(it.normalized, normalized) } ?: return false
        return nowMs - previous.atMs >= STABILITY_MS
    }

    private fun isMusicChatter(
        normalized: String,
        nowMs: Long,
    ): Boolean {
        val window = recent.filter { nowMs - it.atMs <= CHATTER_WINDOW_MS }
        val unrelated =
            window
                .map { it.normalized }
                .distinct()
                .count { !isSameUtterance(it, normalized) }
        return unrelated >= CHATTER_DISTINCT_LIMIT
    }

    private fun looksLikeImmediateCommand(normalized: String): Boolean {
        val words = normalized.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty() || words.size > MAX_IMMEDIATE_WORDS) return false
        return words.maxOf { word -> word.count { it.isLetter() } } >= MIN_LONGEST_WORD_LETTERS
    }

    private fun remember(
        normalized: String,
        nowMs: Long,
    ) {
        if (normalized.isEmpty()) return
        recent.addLast(RecentHypothesis(normalized, nowMs))
        prune(nowMs)
    }

    private fun prune(nowMs: Long) {
        while (recent.isNotEmpty() && nowMs - recent.first().atMs > CHATTER_WINDOW_MS) {
            recent.removeFirst()
        }
    }

    private fun isSameUtterance(
        a: String,
        b: String,
    ): Boolean {
        if (a == b) return true
        val (shorter, longer) = if (a.length <= b.length) a to b else b to a
        if (shorter.length < MIN_PREFIX_CHARS) return false
        return longer.startsWith(shorter)
    }

    private fun normalize(text: String): String = text.trim().lowercase().replace(WHITESPACE, " ")

    private data class RecentHypothesis(
        val normalized: String,
        val atMs: Long,
    )

    companion object {
        const val WINDOW_MS = 2_000L
        const val MIN_TEXT_CHARS = 4
        const val MIN_LETTER_CHARS = 3
        const val MIN_DUCK_TOKEN_PROB = 0.40f
        const val CHATTER_WINDOW_MS = 900L
        const val CHATTER_DISTINCT_LIMIT = 3
        const val STABILITY_MS = 200L
        const val MAX_IMMEDIATE_WORDS = 2
        const val MIN_LONGEST_WORD_LETTERS = 4
        private const val MIN_PREFIX_CHARS = 3
        private val WHITESPACE = Regex("\\s+")

        fun shouldKeepPaused(
            intentId: String,
            slots: Map<String, String>,
        ): Boolean {
            if (intentId == "STOP") return true
            if (intentId != "MEDIA_CTRL") return false
            val command = slots["command"].orEmpty().trim().lowercase()
            if (command.isBlank()) return false
            return command.contains("pause") ||
                command.contains("pauza") ||
                command.contains("stop") ||
                command.contains("zastav")
        }
    }
}
