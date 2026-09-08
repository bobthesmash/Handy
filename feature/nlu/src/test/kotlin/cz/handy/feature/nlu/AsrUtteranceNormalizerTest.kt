package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals

class AsrUtteranceNormalizerTest {
    @Test
    fun strips_leading_oh_hey_and_collapses_space() {
        assertEquals("light off", AsrUtteranceNormalizer.normalize("OH LIGHT OFF"))
        assertEquals("light off", AsrUtteranceNormalizer.normalize("  hey   light   off "))
        assertEquals("light off", AsrUtteranceNormalizer.normalize("oh hey light off"))
    }

    @Test
    fun expands_english_contractions() {
        assertEquals("what is my battery", AsrUtteranceNormalizer.normalize("what's my battery"))
        assertEquals("what is my battery", AsrUtteranceNormalizer.normalize("WHATS MY BATTERY"))
        assertEquals("how is my battery", AsrUtteranceNormalizer.normalize("how's my battery"))
    }

    @Test
    fun leaves_czech_commands_intact() {
        assertEquals("zavolej mamince", AsrUtteranceNormalizer.normalize("  ZAVOLEJ   mamince  "))
    }
}
