package cz.handy.core.common.audio

/**
 * On-device hot-tuner policy for the ASR confidence gate.
 *
 * Wire format (JSON object): `enabled`, `mode` (`prob` | `logprob` | `off`), `minProb`.
 * Missing or invalid files/fields fall back to [DEFAULT] — auto-detect log-probs, threshold 0.28.
 */
data class AsrGatePolicy(
    val enabled: Boolean = true,
    val mode: AsrGateMode = AsrGateMode.AUTO,
    val minProb: Float = AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB,
) {
    companion object {
        val DEFAULT: AsrGatePolicy = AsrGatePolicy()

        fun parseJson(raw: String?): AsrGatePolicy = AsrGateJson.parse(raw)
    }
}

enum class AsrGateMode {
    /** Treat scores outside `[0, 1]` as log-probs (`exp`); values in range stay probabilities. */
    AUTO,

    /** Compare the raw score to [AsrGatePolicy.minProb] in probability space. */
    PROB,

    /** Always `exp(score)` before comparing to [AsrGatePolicy.minProb]. */
    LOGPROB,

    /** Never drop on confidence (same as `enabled=false`). */
    OFF,
    ;

    val wireName: String
        get() =
            when (this) {
                AUTO -> "auto"
                PROB -> "prob"
                LOGPROB -> "logprob"
                OFF -> "off"
            }

    companion object {
        fun fromWire(raw: String): AsrGateMode? =
            when (raw.trim().lowercase()) {
                "auto" -> AUTO
                "prob" -> PROB
                "logprob" -> LOGPROB
                "off" -> OFF
                else -> null
            }
    }
}

internal object AsrGateJson {
    fun parse(raw: String?): AsrGatePolicy {
        if (raw.isNullOrBlank()) return AsrGatePolicy.DEFAULT
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return AsrGatePolicy.DEFAULT
        return runCatching {
            AsrGatePolicy(
                enabled = booleanField(trimmed, "enabled") ?: AsrGatePolicy.DEFAULT.enabled,
                mode =
                    rawField(trimmed, "mode")?.let { AsrGateMode.fromWire(it) }
                        ?: AsrGatePolicy.DEFAULT.mode,
                minProb = floatField(trimmed, "minProb") ?: AsrGatePolicy.DEFAULT.minProb,
            )
        }.getOrDefault(AsrGatePolicy.DEFAULT)
    }

    private fun booleanField(
        json: String,
        key: String,
    ): Boolean? =
        when (rawField(json, key)?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }

    private fun floatField(
        json: String,
        key: String,
    ): Float? = rawField(json, key)?.toFloatOrNull()

    private fun rawField(
        json: String,
        key: String,
    ): String? {
        val needle = "\"$key\""
        val keyAt = json.indexOf(needle)
        if (keyAt < 0) return null
        val colon = json.indexOf(':', startIndex = keyAt + needle.length)
        if (colon < 0) return null
        var i = colon + 1
        while (i < json.length && json[i].isWhitespace()) {
            i++
        }
        if (i >= json.length) return null
        return if (json[i] == '"') {
            val end = json.indexOf('"', startIndex = i + 1)
            if (end < 0) null else json.substring(i + 1, end)
        } else {
            val end = json.indexOfValueEnd(i)
            json.substring(i, end).takeIf { it.isNotEmpty() }
        }
    }

    private fun String.indexOfValueEnd(from: Int): Int {
        var i = from
        while (i < length) {
            val ch = this[i]
            if (ch == ',' || ch == '}' || ch.isWhitespace()) return i
            i++
        }
        return length
    }
}
