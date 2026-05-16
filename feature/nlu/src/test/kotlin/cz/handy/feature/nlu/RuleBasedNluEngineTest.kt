package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class RuleBasedNluEngineTest {
    private val engine = RuleBasedNluEngine(HandyNluCatalogs.mvp)

    @Test
    fun torch_onAndOff() {
        assertIntent(
            "TORCH",
            mapOf("mode" to "zapni"),
            engine.parse("zapni baterku"),
        )
        assertIntent(
            "TORCH",
            mapOf("mode" to "vypni"),
            engine.parse("  VYPNI   baterku "),
        )
    }

    @Test
    fun volume_and_alarm_and_notif() {
        assertIntent(
            "VOLUME",
            mapOf("operation" to "zvyš"),
            engine.parse("zvyš hlasitost"),
        )
        assertIntent("VOLUME", emptyMap(), engine.parse("úplně ztiš"))
        assertIntent(
            "SET_ALARM",
            mapOf("time" to "7:30"),
            engine.parse("budík na 7:30"),
        )
        assertIntent(
            "READ_LAST_NOTIFICATION",
            emptyMap(),
            engine.parse("přečti poslední notifikaci"),
        )
    }

    @Test
    fun call_extractsContact() {
        val r = engine.parse("zavolej mamince")
        val m = assertIs<NluResult.Matched>(r)
        assertEquals("CALL", m.intent.intentId)
        assertEquals("mamince", m.intent.slots["contact"])
        assertEquals(true, m.intent.requiresConfirm)
    }

    @Test
    fun sms_twoSlots() {
        val r = engine.parse("sms pro bratrovi text dorazím později")
        val m = assertIs<NluResult.Matched>(r)
        assertEquals("SEND_SMS", m.intent.intentId)
        assertEquals("bratrovi", m.intent.slots["contact"])
        assertEquals("dorazím později", m.intent.slots["message"])
    }

    @Test
    fun no_match_blank() {
        assertEquals(NluResult.NoMatch, engine.parse("   "))
        assertEquals(NluResult.NoMatch, engine.parse("nesmysl který nepasuje"))
    }

    @Test
    fun dsl_rejectsDuplicateIntentIds() {
        assertFailsWith<IllegalArgumentException> {
            intentCatalog {
                intent("DUP") { phrase("alfa") }
                intent("DUP") { phrase("beta") }
            }
        }
    }

    @Test
    fun dsl_slotMustAppearInPhrase() {
        assertFailsWith<IllegalArgumentException> {
            intentCatalog {
                intent("BAD") {
                    phrase("jen text bez slotů")
                    slot("ghost") {}
                }
            }
        }
    }

    @Test
    fun dsl_bracePhraseMustDeclareNonEmptyPatterns() {
        assertFailsWith<IllegalArgumentException> {
            intentCatalog {
                intent("EMPTY") {}
            }
        }
    }

    private fun assertIntent(
        id: String,
        slots: Map<String, String>,
        result: NluResult,
    ) {
        val m = assertIs<NluResult.Matched>(result)
        assertEquals(id, m.intent.intentId)
        assertEquals(slots, m.intent.slots)
    }
}
