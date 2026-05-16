package cz.handy.feature.voiceid.enrollment

import kotlin.math.sqrt

/** Maps a PCM16 window to roughly 0f…1f for lightweight level meters ([F1-T01]). */
object PcmIntensity {
    fun rmsNormalized01(
        samples: ShortArray,
        offset: Int,
        length: Int,
    ): Float {
        require(offset >= 0 && length >= 0 && offset + length <= samples.size)
        if (length == 0) return 0f
        var sum = 0L
        for (i in offset until offset + length) {
            val s = samples[i].toLong()
            sum += s * s
        }
        val rms = sqrt(sum.toDouble() / length)
        return (rms / Short.MAX_VALUE).coerceIn(0.0, 1.0).toFloat()
    }
}
