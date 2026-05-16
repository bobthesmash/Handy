package cz.handy.core.audio

/**
 * Thread-safe rolling buffer for 16-bit mono PCM: keeps the last [capacitySamples] samples.
 * Single-writer (capture thread) is assumed; [copyOldestFirst] is safe for readers.
 */
class MonoPcmRingBuffer(
    val capacitySamples: Int,
) {
    init {
        require(capacitySamples > 0) { "capacitySamples must be positive" }
    }

    private val buf = ShortArray(capacitySamples)
    private var writePos = 0
    private var totalWritten = 0L

    @Synchronized
    fun write(
        src: ShortArray,
        offset: Int,
        length: Int,
    ) {
        require(
            offset >= 0 && length >= 0 && offset + length <= src.size,
        )
        for (i in 0 until length) {
            buf[writePos] = src[offset + i]
            writePos = (writePos + 1) % capacitySamples
            totalWritten++
        }
    }

    fun write(src: ShortArray) = write(src, 0, src.size)

    /** Počet doposud zapsaných vzorků (neklesá; pro inkrementální konsumenty). */
    @Synchronized
    fun totalSamplesWritten(): Long = totalWritten

    /**
     * Vzorky novější než [markerExclusive] (exkluzivně), v chronologickém pořadí.
     * Při zpoždění čteče starší než [capacitySamples] se ořízne na okno bufferu.
     * @return Nová známka (vždy aktuální [totalWritten] po tomto čtení).
     */
    @Synchronized
    fun consumeMono16SinceTotalWritten(markerExclusive: Long): Pair<ShortArray, Long> {
        val tw = totalWritten
        if (tw <= markerExclusive) {
            return Pair(ShortArray(0), markerExclusive)
        }
        val oldestAbsolute = maxOf(0L, tw - capacitySamples)
        val firstAbs = maxOf(markerExclusive, oldestAbsolute)
        val count = (tw - firstAbs).toInt()
        if (count <= 0) {
            return Pair(ShortArray(0), markerExclusive)
        }
        val out = ShortArray(count)
        if (tw < capacitySamples) {
            for (i in 0 until count) {
                out[i] = buf[(firstAbs + i).toInt()]
            }
        } else {
            for (i in 0 until count) {
                val absIdx = firstAbs + i
                val k = (absIdx - oldestAbsolute).toInt()
                val bufIdx = (writePos + k) % capacitySamples
                out[i] = buf[bufIdx]
            }
        }
        return Pair(out, tw)
    }

    /** Oldest → newest (length = min(totalWritten, capacity)). */
    @Synchronized
    fun copyOldestFirst(): ShortArray {
        val filled = minOf(totalWritten, capacitySamples.toLong()).toInt()
        if (filled == 0) return ShortArray(0)

        val out = ShortArray(filled)
        if (totalWritten < capacitySamples) {
            System.arraycopy(buf, 0, out, 0, filled)
            return out
        }

        val oldest = writePos
        val firstLen = capacitySamples - oldest
        System.arraycopy(buf, oldest, out, 0, firstLen)
        if (firstLen < filled) {
            System.arraycopy(buf, 0, out, firstLen, writePos)
        }
        return out
    }
}
