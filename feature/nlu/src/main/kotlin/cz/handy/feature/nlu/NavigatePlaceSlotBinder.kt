package cz.handy.feature.nlu

import cz.handy.core.common.asr.PlaceAsrLanguage

/**
 * After English command NLU matches `NAVIGATE`, replace `{place}` with the
 * location-language ASR tail when one exists. Names are never fuzzy-rewritten.
 */
object NavigatePlaceSlotBinder {
    fun bind(
        intent: ParsedIntent,
        language: PlaceAsrLanguage,
        locationPlace: String?,
    ): ParsedIntent {
        if (intent.intentId != "NAVIGATE") return intent
        if (language != PlaceAsrLanguage.CZECH) return intent
        val place = locationPlace?.trim().orEmpty()
        if (place.isEmpty()) return intent
        return intent.copy(slots = intent.slots + ("place" to place))
    }
}
