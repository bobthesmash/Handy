package cz.handy.feature.nlu

import cz.handy.feature.nlu.internal.IntentDefinition
import cz.handy.feature.nlu.internal.PhraseMatcherSpec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NluEngineEdgeCasesTest {
    @Test
    fun skipsMatchWhenRequiredSlotBlank_triesNextIntent() {
        val first =
            IntentDefinition(
                id = "FIRST",
                requiresConfirm = false,
                matchers =
                    listOf(
                        PhraseMatcherSpec(
                            Regex("^a(\\d*)b$"),
                            listOf("x"),
                        ),
                    ),
                slotRequired = mapOf("x" to true),
            )
        val second =
            IntentDefinition(
                id = "SECOND",
                requiresConfirm = false,
                matchers =
                    listOf(
                        PhraseMatcherSpec(
                            Regex("^a(\\d*)b$"),
                            listOf("x"),
                        ),
                    ),
                slotRequired = mapOf("x" to false),
            )
        val engine = RuleBasedNluEngine(IntentCatalog(listOf(first, second)))
        val r = engine.blockingParse("ab")
        val m = assertIs<NluResult.Matched>(r)
        assertEquals("SECOND", m.intent.intentId)
        assertEquals("", m.intent.slots["x"])
    }

    @Test
    fun skipsWhenRegexGroupsMismatchOrderedSlots() {
        val def =
            IntentDefinition(
                id = "BROKEN",
                requiresConfirm = false,
                matchers =
                    listOf(
                        PhraseMatcherSpec(
                            Regex("^(.+)$"),
                            listOf("a", "b"),
                        ),
                    ),
                slotRequired = mapOf("a" to true, "b" to true),
            )
        val catalog = IntentCatalog(listOf(def))
        val engine = RuleBasedNluEngine(catalog)
        assertEquals(NluResult.NoMatch, engine.blockingParse("jen jedna grupa"))
    }

    @Test
    fun mvp_requiresConfirmFlags() {
        val engine = RuleBasedNluEngine(HandyNluCatalogs.mvp)

        fun needsConfirm(phrase: String) = (engine.blockingParse(phrase) as NluResult.Matched).intent.requiresConfirm

        assertEquals(true, needsConfirm("zavolej alfa"))
        assertEquals(true, needsConfirm("pošli sms alfa že beta"))
        assertEquals(true, needsConfirm("budík na sedm"))
        assertEquals(true, needsConfirm("odpověz že test"))

        assertEquals(false, needsConfirm("zapni baterku"))
        assertEquals(false, needsConfirm("ztiš hlasitost"))
        assertEquals(false, needsConfirm("přečti poslední notifikaci"))
        assertEquals(false, needsConfirm("kolik je hodin"))
        assertEquals(false, needsConfirm("jaký je den"))
        assertEquals(false, needsConfirm("stav baterie"))
        assertEquals(false, needsConfirm("přehraj hudbu"))
        assertEquals(false, needsConfirm("další hudba"))
        assertEquals(false, needsConfirm("otevři chrome"))
        assertEquals(false, needsConfirm("naviguj na brno"))
        assertEquals(false, needsConfirm("časovač pět minut"))
        assertEquals(false, needsConfirm("zruš"))
        assertEquals(false, needsConfirm("ticho"))
        assertEquals(false, needsConfirm("zopakuj"))
        assertEquals(false, needsConfirm("nazývej Jan Novák jako bratr"))
        assertEquals(false, needsConfirm("smaž alias bratr"))
    }
}
