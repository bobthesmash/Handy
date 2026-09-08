package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhraseSpellingToleranceTest {
    @Test
    fun butter_matches_battery_at_min_confidence_80() {
        assertTrue(PhraseSpellingTolerance.within("butter", "battery"))
        assertTrue(PhraseSpellingTolerance.score("butter", "battery") >= PhraseSpellingTolerance.MIN_CONFIDENCE)
        assertTrue(PhraseSpellingTolerance.within("batteret", "battery"))
        assertTrue(PhraseSpellingTolerance.score("batteret", "battery") >= PhraseSpellingTolerance.MIN_CONFIDENCE)
    }

    @Test
    fun flesh_light_matches_flashlight() {
        assertTrue(PhraseSpellingTolerance.within("flesh light", "flashlight"))
        assertTrue(PhraseSpellingTolerance.within("fleshlight", "flashlight"))
        assertTrue(
            PhraseSpellingTolerance.score("flesh light", "flashlight") >=
                PhraseSpellingTolerance.MIN_CONFIDENCE,
        )
    }

    @Test
    fun light_of_matches_light_off() {
        assertTrue(PhraseSpellingTolerance.within("light of", "light off"))
        assertTrue(
            PhraseSpellingTolerance.score("light of", "light off") >=
                PhraseSpellingTolerance.MIN_CONFIDENCE,
        )
    }

    @Test
    fun caal_matches_call_prefix() {
        assertTrue(PhraseSpellingTolerance.within("caal", "call"))
    }

    @Test
    fun token_sort_ignores_word_order() {
        assertTrue(PhraseSpellingTolerance.within("battery my what is", "what is my battery"))
    }

    @Test
    fun wratio_partial_is_not_used_on_short_catalog_phrases() {
        assertFalse(PhraseSpellingTolerance.within("battery status", "battery"))
        assertTrue(
            PhraseSpellingTolerance.score("battery status", "battery") <
                PhraseSpellingTolerance.MIN_CONFIDENCE,
        )
    }

    @Test
    fun exact_match_and_empty() {
        assertTrue(PhraseSpellingTolerance.within("pause", "pause"))
        assertTrue(PhraseSpellingTolerance.within("", ""))
        assertFalse(PhraseSpellingTolerance.within("pause", ""))
        assertFalse(PhraseSpellingTolerance.within("", "pause"))
    }

    @Test
    fun unrelated_phrases_do_not_match() {
        assertFalse(PhraseSpellingTolerance.within("butter", "flashlight"))
        assertFalse(PhraseSpellingTolerance.within("open chrome", "battery"))
        assertFalse(PhraseSpellingTolerance.within("navigate to prague", "what time is it"))
    }

    @Test
    fun short_tokens_stay_strict() {
        assertFalse(PhraseSpellingTolerance.within("no", "ok"))
        assertFalse(PhraseSpellingTolerance.within("on", "off"))
    }

    @Test
    fun contact_like_tokens_are_not_rewritten_by_command_scorer() {
        assertFalse(PhraseSpellingTolerance.within("jane", "john"))
        assertFalse(PhraseSpellingTolerance.within("austin", "boston"))
        assertTrue(PhraseSpellingTolerance.score("jane", "john") < PhraseSpellingTolerance.MIN_CONFIDENCE)
        assertTrue(PhraseSpellingTolerance.score("austin", "boston") < PhraseSpellingTolerance.MIN_CONFIDENCE)
        // mitchell/michael can sit near cutoff; NLU still copies the spoken name.
    }
}
