package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/**
 * Live S23 Sherpa EN mishears: command phrases get ~20% spelling tolerance;
 * contact/place slot values stay exact.
 */
class AsrSpellingNluEngineTest {
    private val engine = RuleBasedNluEngine(HandyNluCatalogs.mvp)

    @Test
    fun spoken_battery_near_misses_match_what_battery() {
        assertIntent("WHAT_BATTERY", engine.blockingParse("battery"))
        assertIntent("WHAT_BATTERY", engine.blockingParse("BUTTER"))
        assertIntent("WHAT_BATTERY", engine.blockingParse("BATTERET"))
        assertIntent("WHAT_BATTERY", engine.blockingParse("WHAT IS MY BUTTER"))
        assertIntent("WHAT_BATTERY", engine.blockingParse("WHAT IS MY BATTERET"))
        assertIntent("WHAT_BATTERY", engine.blockingParse("what's my battery"))
    }

    @Test
    fun spoken_flashlight_and_light_off_near_misses_match_torch() {
        assertIntent("TORCH", engine.blockingParse("flashlight"))
        assertIntent("TORCH", engine.blockingParse("FLESH LIGHT"))
        assertIntent("TORCH", engine.blockingParse("LIGHT OF"))
        assertIntent("TORCH", engine.blockingParse("OH LIGHT OFF"))
        val off = assertIs<NluResult.Matched>(engine.blockingParse("OH LIGHT OFF"))
        assertEquals("off", off.intent.slots["mode"])
    }

    @Test
    fun typed_console_exact_phrases_still_match() {
        assertIntent("WHAT_BATTERY", engine.blockingParse("battery"))
        assertIntent("PLAY_MEDIA", engine.blockingParse("play"))
        assertIntent("MEDIA_CTRL", engine.blockingParse("pause"))
        assertIntent("WHAT_TIME", engine.blockingParse("what time is it"))
        assertIntent("WHAT_DATE", engine.blockingParse("what is the date"))
    }

    @Test
    fun call_keeps_wrong_contact_name_exact() {
        val butter = assertIs<NluResult.Matched>(engine.blockingParse("call butter"))
        assertEquals("CALL", butter.intent.intentId)
        assertEquals("butter", butter.intent.slots["contact"])

        val jane = assertIs<NluResult.Matched>(engine.blockingParse("call jane"))
        assertEquals("jane", jane.intent.slots["contact"])
        assertNotEquals("john", jane.intent.slots["contact"])

        val mitchell = assertIs<NluResult.Matched>(engine.blockingParse("call mitchell"))
        assertEquals("mitchell", mitchell.intent.slots["contact"])
        assertNotEquals("michael", mitchell.intent.slots["contact"])

        val prefixTypo = assertIs<NluResult.Matched>(engine.blockingParse("caal jane"))
        assertEquals("CALL", prefixTypo.intent.intentId)
        assertEquals("jane", prefixTypo.intent.slots["contact"])
        assertEquals(setOf("contact", "place", "target"), CatalogPhraseFuzzyMatcher.NAME_SLOTS)
    }

    @Test
    fun navigate_keeps_wrong_place_name_exact() {
        val austin = assertIs<NluResult.Matched>(engine.blockingParse("navigate to austin"))
        assertEquals("NAVIGATE", austin.intent.intentId)
        assertEquals("austin", austin.intent.slots["place"])
        assertNotEquals("boston", austin.intent.slots["place"])

        val boston = assertIs<NluResult.Matched>(engine.blockingParse("navigate to boston"))
        assertEquals("boston", boston.intent.slots["place"])
    }

    @Test
    fun fuzzy_does_not_steal_name_bearing_utterances_into_battery() {
        val call = assertIs<NluResult.Matched>(engine.blockingParse("call batteret"))
        assertEquals("CALL", call.intent.intentId)
        assertEquals("batteret", call.intent.slots["contact"])
    }

    private fun assertIntent(
        id: String,
        result: NluResult,
    ) {
        val m = assertIs<NluResult.Matched>(result)
        assertEquals(id, m.intent.intentId)
    }
}
