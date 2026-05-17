package cz.handy.feature.asr

/**
 * Společný kontrakt pro streamování 16-bit mono 16 kHz (Sherpa zipformer2 nebo Vosk CZ).
 */
interface StreamingAsrRecognizer : AutoCloseable {
    fun startUtterance()

    fun appendPcm16Mono(pcm: ShortArray): StreamingSherpaTick
}

fun StreamingAsrRecognizer.decodeMono16StoredUtterance(samples: ShortArray): String {
    if (samples.isEmpty()) return ""
    val step =
        (cz.handy.core.audio.MicCaptureConfig.SAMPLE_RATE_HZ / 20).coerceAtLeast(400)
    startUtterance()
    var last = ""
    var offset = 0
    while (offset < samples.size) {
        val end = kotlin.math.min(offset + step, samples.size)
        val chunk = samples.copyOfRange(offset, end)
        val tick = appendPcm16Mono(chunk)
        val t = tick.text.trim()
        if (t.isNotEmpty()) last = t
        offset = end
    }
    return last.trim()
}
