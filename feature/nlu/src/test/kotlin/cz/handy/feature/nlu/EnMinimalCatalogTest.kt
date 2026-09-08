package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class EnMinimalCatalogTest {
    private val engine = RuleBasedNluEngine(HandyNluCatalogs.enMinimal)

    @Test
    fun play_pause_torch_battery_volume_phrases_are_present() {
        assertIntent("PLAY_MEDIA", engine.blockingParse("play"))
        assertIntent("PLAY_MEDIA", engine.blockingParse("play music"))
        assertIntent("MEDIA_CTRL", engine.blockingParse("pause"))
        assertIntent("TORCH", engine.blockingParse("flashlight"))
        assertIntent("TORCH", engine.blockingParse("light off"))
        assertIntent("WHAT_BATTERY", engine.blockingParse("battery"))
        assertIntent("VOLUME", engine.blockingParse("volume up"))
        assertIntent("VOLUME", engine.blockingParse("volume down"))
    }

    @Test
    fun stop_pauses_media_without_using_mute() {
        assertIntent("STOP", engine.blockingParse("stop"))
        val mute = engine.blockingParse("mute")
        assertNotEquals(
            "STOP",
            (mute as? NluResult.Matched)?.intent?.intentId,
            "standby Mute must not be a media STOP phrase",
        )
        val silence = engine.blockingParse("silence")
        assertNotEquals(
            "STOP",
            (silence as? NluResult.Matched)?.intent?.intentId,
            "silence must not fuzzy-match media STOP",
        )
        val quiet = engine.blockingParse("quiet")
        assertNotEquals(
            "STOP",
            (quiet as? NluResult.Matched)?.intent?.intentId,
            "quiet must not fuzzy-match media STOP",
        )
        assertNotEquals(
            "STOP",
            (RuleBasedNluEngine(HandyNluCatalogs.mvp).blockingParse("be quiet") as? NluResult.Matched)
                ?.intent
                ?.intentId,
            "mvp hush phrases must not pause media via STOP",
        )
    }

    @Test
    fun navigate_keeps_place_slot() {
        val out = assertIs<NluResult.Matched>(engine.blockingParse("navigate to austin"))
        assertEquals("NAVIGATE", out.intent.intentId)
        assertEquals("austin", out.intent.slots["place"])
    }

    private fun assertIntent(
        id: String,
        result: NluResult,
    ) {
        val m = assertIs<NluResult.Matched>(result)
        assertEquals(id, m.intent.intentId)
    }
}
