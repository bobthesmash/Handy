package cz.handy.feature.wakeword

import ai.picovoice.porcupine.Porcupine

internal fun concatShortArrays(
    left: ShortArray,
    right: ShortArray,
): ShortArray {
    if (right.isEmpty()) {
        return left
    }
    if (left.isEmpty()) {
        return right
    }
    val out = ShortArray(left.size + right.size)
    System.arraycopy(left, 0, out, 0, left.size)
    System.arraycopy(right, 0, out, left.size, right.size)
    return out
}

/**
 * Skládá stream PCM do rámců o délce [frameLengthSamples] ([Porcupine.process] vstup).
 */
internal class PorcupinePcmFrameAccumulator(
    private val frameLengthSamples: Int,
) {
    private var pending = ShortArray(0)

    fun append(chunk: ShortArray) {
        if (chunk.isEmpty()) return
        pending = concatShortArrays(pending, chunk)
    }

    /** Zkopíruje další rámec do [dest]; musí mít přesnou délku rámu. */
    fun pollFrame(dest: ShortArray): Boolean {
        require(dest.size == frameLengthSamples)
        if (pending.size < frameLengthSamples) {
            return false
        }
        System.arraycopy(pending, 0, dest, 0, frameLengthSamples)
        val restLen = pending.size - frameLengthSamples
        pending =
            if (restLen <= 0) {
                ShortArray(0)
            } else {
                pending.copyOfRange(frameLengthSamples, pending.size)
            }
        return true
    }

    fun clear() {
        pending = ShortArray(0)
    }
}
