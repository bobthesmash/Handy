package cz.handy.feature.asr

import cz.handy.core.common.asr.PlaceAsrLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigatePlaceCascadeTest {
    @Test
    fun english_country_does_not_decode_czech_tail() {
        val pcm = ShortArray(1600) { 1 }
        val out =
            NavigatePlaceCascade.czechPlaceOrNull(
                language = PlaceAsrLanguage.ENGLISH,
                commandEngineLanguage = PlaceAsrLanguage.ENGLISH,
                utterancePcm = pcm,
                prefixEndSample = 800,
                prefixWordCount = 2,
                englishWordCount = 4,
                decode = { "praha" },
            )
        assertNull(out)
    }

    @Test
    fun czech_country_decodes_tail_after_prefix() {
        val pcm = ShortArray(1600) { 1 }
        val out =
            NavigatePlaceCascade.czechPlaceOrNull(
                language = PlaceAsrLanguage.CZECH,
                commandEngineLanguage = PlaceAsrLanguage.ENGLISH,
                utterancePcm = pcm,
                prefixEndSample = 800,
                prefixWordCount = 2,
                englishWordCount = 4,
                decode = { samples ->
                    assertEquals(800, samples.size)
                    "Karlův most"
                },
            )
        assertEquals("Karlův most", out)
    }

    @Test
    fun already_czech_command_engine_skips_cascade() {
        val pcm = ShortArray(800) { 1 }
        val out =
            NavigatePlaceCascade.czechPlaceOrNull(
                language = PlaceAsrLanguage.CZECH,
                commandEngineLanguage = PlaceAsrLanguage.CZECH,
                utterancePcm = pcm,
                prefixEndSample = 100,
                prefixWordCount = 2,
                englishWordCount = 3,
                decode = { "should not run" },
            )
        assertNull(out)
    }

    @Test
    fun missing_pcm_skips_cascade() {
        val out =
            NavigatePlaceCascade.czechPlaceOrNull(
                language = PlaceAsrLanguage.CZECH,
                commandEngineLanguage = PlaceAsrLanguage.ENGLISH,
                utterancePcm = null,
                prefixEndSample = null,
                prefixWordCount = 2,
                englishWordCount = 3,
                decode = { "praha" },
            )
        assertNull(out)
    }

    @Test
    fun blank_czech_decode_returns_null_so_english_place_is_kept() {
        val pcm = ShortArray(800) { 1 }
        val out =
            NavigatePlaceCascade.czechPlaceOrNull(
                language = PlaceAsrLanguage.CZECH,
                commandEngineLanguage = PlaceAsrLanguage.ENGLISH,
                utterancePcm = pcm,
                prefixEndSample = 100,
                prefixWordCount = 2,
                englishWordCount = 3,
                decode = { "   " },
            )
        assertNull(out)
    }
}
