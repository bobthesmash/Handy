package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EnglishOverlayNlUTest {
    private val csOnly = RuleBasedNluEngine(HandyNluCatalogs.mvp)
    private val chained =
        ChainedUtteranceParsers(
            RuleBasedNluEngine(HandyNluCatalogs.mvp),
            RuleBasedNluEngine(HandyNluCatalogs.enMinimal),
        )

    @Test
    fun english_overlay_matches_when_cs_misses() {
        assertEquals(NluResult.NoMatch, csOnly.blockingParse("what time is it"))
        val out = chained.blockingParse("what time is it")
        val m = assertIs<NluResult.Matched>(out)
        assertEquals("WHAT_TIME", m.intent.intentId)
    }

    @Test
    fun czech_still_preferred_before_english() {
        val out = chained.blockingParse("kolik je hodin")
        val m = assertIs<NluResult.Matched>(out)
        assertEquals("WHAT_TIME", m.intent.intentId)
    }
}
