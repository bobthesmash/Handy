package cz.handy.feature.asr

import cz.handy.core.common.asr.PlaceAsrLanguage

/**
 * Cascaded NAVIGATE place capture: English command ASR, then location-selected
 * Czech Vosk on the place tail when the user is in CZ/SK.
 */
object NavigatePlaceCascade {
    fun czechPlaceOrNull(
        language: PlaceAsrLanguage,
        commandEngineLanguage: PlaceAsrLanguage,
        utterancePcm: ShortArray?,
        prefixEndSample: Int?,
        prefixWordCount: Int,
        englishWordCount: Int,
        decode: (ShortArray) -> String,
    ): String? {
        val pcm = utterancePcm
        if (language != PlaceAsrLanguage.CZECH) return null
        if (commandEngineLanguage == PlaceAsrLanguage.CZECH) return null
        if (pcm == null || pcm.isEmpty()) return null
        val tail = NavigatePlaceTailPcm.slice(pcm, prefixEndSample, prefixWordCount, englishWordCount)
        if (tail.isEmpty()) return null
        val decoded = runCatching { decode(tail) }.getOrDefault("")
        val tailOnly = NavigatePlaceTailPcm.slicedTailOnly(pcm, prefixEndSample)
        val place = NavigatePlaceText.placeFromCzechDecode(decoded, tailOnly, prefixWordCount)
        return place.takeIf { it.isNotBlank() }
    }
}
