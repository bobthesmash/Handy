package cz.handy.feature.tts

import java.util.concurrent.atomic.AtomicInteger

/**
 * Zajišťuje, že completion handler starého utterance se nespustí po [stop] nebo novém [speak].
 */
internal class UtteranceSequenceGate {
    private val generation = AtomicInteger(0)

    fun nextUtteranceId(): Int = generation.incrementAndGet()

    fun bumpOnStop() {
        generation.incrementAndGet()
    }

    fun isCurrentUtterance(id: Int): Boolean = id == generation.get()
}
