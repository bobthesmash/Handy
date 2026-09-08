package cz.handy.feature.asr

import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatePlaceTextTest {
    @Test
    fun tail_only_decode_is_the_place() {
        assertEquals(
            "Karlův most",
            NavigatePlaceText.placeFromCzechDecode(
                czechText = "  Karlův most ",
                decodedTailOnly = true,
                englishPrefixWordCount = 2,
            ),
        )
    }

    @Test
    fun full_utterance_drops_english_prefix_word_count() {
        assertEquals(
            "brno hlavní nádraží",
            NavigatePlaceText.placeFromCzechDecode(
                czechText = "navigate to brno hlavní nádraží",
                decodedTailOnly = false,
                englishPrefixWordCount = 2,
            ),
        )
    }

    @Test
    fun blank_decode_is_empty() {
        assertEquals(
            "",
            NavigatePlaceText.placeFromCzechDecode(
                czechText = "  ",
                decodedTailOnly = true,
                englishPrefixWordCount = 2,
            ),
        )
    }
}
