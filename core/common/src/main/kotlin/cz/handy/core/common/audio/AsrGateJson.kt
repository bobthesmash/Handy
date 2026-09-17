package cz.handy.core.common.audio

/**
 * Parses `asr_gate.json`. Missing or invalid fields fall back to [AsrGatePolicy.DEFAULT]
 * so a bad adb-push cannot crash the assistant.
 */
object AsrGateJson {
    fun parse(text: String): AsrGatePolicy {
        val json = text.trim()
        if (json.isEmpty()) return AsrGatePolicy.DEFAULT
        return runCatching {
            AsrGatePolicy(
                enabled = boolField(json, "enabled") ?: AsrGatePolicy.DEFAULT.enabled,
                mode = AsrGateMode.fromWire(stringField(json, "mode")),
                minProb = floatField(json, "minProb") ?: AsrHypothesisConfidence.DEFAULT_MIN_TOKEN_PROB,
            )
        }.getOrElse { AsrGatePolicy.DEFAULT }
    }

    private fun stringField(
        json: String,
        name: String,
    ): String? =
        Regex(""""$name"\s*:\s*"([^"]*)"""")
            .find(json)
            ?.groupValues
            ?.getOrNull(1)

    private fun boolField(
        json: String,
        name: String,
    ): Boolean? {
        val raw =
            Regex(""""$name"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE)
                .find(json)
                ?.groupValues
                ?.getOrNull(1)
                ?: return null
        return raw.equals("true", ignoreCase = true)
    }

    private fun floatField(
        json: String,
        name: String,
    ): Float? {
        val raw =
            Regex(""""$name"\s*:\s*(-?\d+(?:\.\d+)?(?:[eE][+-]?\d+)?)""")
                .find(json)
                ?.groupValues
                ?.getOrNull(1)
                ?: return null
        return raw.toFloatOrNull()
    }
}
