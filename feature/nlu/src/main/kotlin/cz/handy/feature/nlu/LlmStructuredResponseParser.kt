package cz.handy.feature.nlu

import org.json.JSONObject

/**
 * Parsuje strukturovaný JSON z lokálního LLM ([F5-T01]) do [ParsedIntent].
 * Neočekává žádný cloud — jen text z inference na zařízení nebo ruční vložení při testech.
 */
object LlmStructuredResponseParser {
    fun parseFromRaw(
        raw: String,
        catalog: IntentCatalog,
    ): NluResult {
        val extracted = extractFirstJsonObject(stripCodeFences(raw)) ?: return NluResult.NoMatch
        return parseJsonObject(extracted, catalog)
    }

    internal fun stripCodeFences(text: String): String {
        val t = text.trim()
        val fence =
            Regex(
                "^```(?:json)?\\s*([\\s\\S]*?)```",
                RegexOption.IGNORE_CASE,
            )
        return fence
            .find(t)
            ?.groupValues
            ?.get(1)
            ?.trim()
            ?: t
    }

    internal fun extractFirstJsonObject(text: String): String? {
        val start = text.indexOf('{')
        if (start < 0) return null
        var depth = 0
        var inString = false
        var escape = false
        for (i in start until text.length) {
            val c = text[i]
            when {
                escape -> escape = false
                c == '\\' && inString -> escape = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> depth++
                c == '}' -> {
                    depth--
                    if (depth == 0) return text.substring(start, i + 1)
                }
            }
        }
        return null
    }

    private fun parseJsonObject(
        json: String,
        catalog: IntentCatalog,
    ): NluResult {
        return try {
            val root = JSONObject(json)
            val intentId = root.optString("intentId").trim()
            if (intentId.isBlank() || intentId !in catalog.intentIds) {
                return NluResult.NoMatch
            }
            val slotsObj = root.optJSONObject("slots") ?: JSONObject()
            val slots = mutableMapOf<String, String>()
            val keys = slotsObj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                slots[k] = slotsObj.optString(k).trim()
            }
            if (!catalog.slotsSatisfied(intentId, slots)) {
                return NluResult.NoMatch
            }
            val confirm =
                when {
                    root.has("requiresConfirm") && !root.isNull("requiresConfirm") ->
                        root.optBoolean("requiresConfirm")
                    else -> catalog.requiresConfirmFor(intentId)
                }
            NluResult.Matched(
                ParsedIntent(
                    intentId = intentId,
                    slots = slots,
                    requiresConfirm = confirm,
                ),
            )
        } catch (_: Exception) {
            NluResult.NoMatch
        }
    }
}
