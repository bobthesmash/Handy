package cz.handy.feature.asr

import cz.handy.core.audio.MicCaptureConfig
import org.vosk.Model
import org.vosk.Recognizer
import java.io.IOException

fun createVoskCzStreamingRecognizer(model: Model): VoskCzStreamingSpeechRecognizer =
    VoskCzStreamingSpeechRecognizer(model)

/**
 * Streamování přes Vosk API (český small model). [minTokenProb] v ticku je vždy `null` — Vosk neposkytuje
 * tokenové pravděpodobnosti jako Sherpa.
 */
class VoskCzStreamingSpeechRecognizer(
    private val model: Model,
) : StreamingAsrRecognizer {
    private var recognizer: Recognizer? = null

    override fun startUtterance() {
        closeRecognizer()
        recognizer = Recognizer(model, MicCaptureConfig.SAMPLE_RATE_HZ.toFloat())
    }

    override fun appendPcm16Mono(pcm: ShortArray): StreamingSherpaTick {
        val rec =
            recognizer
                ?: error("Nejprve zavolej startUtterance().")
        val bytes = pcm.asVoskPcmBytes()
        val endpoint = rec.acceptWaveForm(bytes, bytes.size)
        val json =
            if (endpoint) {
                rec.result
            } else {
                rec.partialResult
            }
        val text = parseVoskJsonText(json)
        if (endpoint) {
            closeRecognizer()
            recognizer = Recognizer(model, MicCaptureConfig.SAMPLE_RATE_HZ.toFloat())
        }
        return StreamingSherpaTick(text = text, endpoint = endpoint, minTokenProb = null)
    }

    override fun close() {
        closeRecognizer()
        model.close()
    }

    private fun closeRecognizer() {
        recognizer?.close()
        recognizer = null
    }
}

@Throws(IOException::class)
fun openVoskCzModel(context: android.content.Context): Model {
    val dir = VoskCzModelAssets.resolveOnDeviceModelDir(context)
    return Model(dir.absolutePath)
}

private fun ShortArray.asVoskPcmBytes(): ByteArray {
    val out = ByteArray(size * 2)
    var i = 0
    for (s in this) {
        out[i++] = (s.toInt() and 0xFF).toByte()
        out[i++] = ((s.toInt() shr 8) and 0xFF).toByte()
    }
    return out
}

internal fun parseVoskJsonText(json: String): String {
    if (json.isBlank()) return ""
    fun field(name: String): String? =
        Regex(""""$name"\s*:\s*"([^"]*)"""")
            .find(json)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    return field("text") ?: field("partial").orEmpty()
}
