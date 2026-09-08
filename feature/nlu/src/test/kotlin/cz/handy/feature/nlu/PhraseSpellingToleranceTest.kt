package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PhraseSpellingToleranceTest {
    @Test
    fun butter_matches_battery_within_about_twenty_percent() {
        assertTrue(PhraseSpellingTolerance.within("butter", "battery"))
        assertTrue(PhraseSpellingTolerance.within("batteret", "battery"))
    }

    @Test
    fun flesh_light_matches_flashlight() {
        assertTrue(PhraseSpellingTolerance.within("flesh light", "flashlight"))
        assertTrue(PhraseSpellingTolerance.within("fleshlight", "flashlight"))
    }

    @Test
    fun light_of_matches_light_off() {
        assertTrue(PhraseSpellingTolerance.within("light of", "light off"))
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
    fun contact_names_are_not_within_command_tolerance() {
        assertFalse(PhraseSpellingTolerance.within("jane", "john"))
        assertFalse(PhraseSpellingTolerance.within("austin", "boston"))
        assertFalse(PhraseSpellingTolerance.within("mitchell", "michael"))
    }
}
