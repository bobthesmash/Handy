package cz.handy.feature.tts

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UtteranceSequenceGateTest {
    @Test
    fun `first utterance is current until bumped`() {
        val g = UtteranceSequenceGate()
        val id = g.nextUtteranceId()
        assertTrue(g.isCurrentUtterance(id))
        g.bumpOnStop()
        assertFalse(g.isCurrentUtterance(id))
    }

    @Test
    fun `new speak invalidates previous id`() {
        val g = UtteranceSequenceGate()
        val first = g.nextUtteranceId()
        val second = g.nextUtteranceId()
        assertFalse(g.isCurrentUtterance(first))
        assertTrue(g.isCurrentUtterance(second))
    }
}
