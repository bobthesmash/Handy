package cz.handy.feature.nlu

import cz.handy.feature.nlu.internal.PhraseTemplateCompiler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntentCatalogAndCompilerTest {
    @Test
    fun mvp_intentIds_documentOrder() {
        assertEquals(
            listOf(
                "CALL",
                "SEND_SMS",
                "SET_ALARM",
                "CANCEL",
                "STOP",
                "REPEAT",
                "VOLUME",
                "READ_LAST_NOTIFICATION",
                "REPLY_NOTIF",
                "PLAY_MEDIA",
                "TORCH",
                "WHAT_TIME",
                "WHAT_DATE",
                "WHAT_BATTERY",
                "OPEN_APP",
                "NAVIGATE",
                "TIMER",
                "SET_CONTACT_ALIAS",
                "REMOVE_CONTACT_ALIAS",
                "MEDIA_CTRL",
            ),
            HandyNluCatalogs.mvp.intentIds,
        )
    }

    @Test
    fun normalizeUtterance_trimsCollapsesAndLowercasesCs() {
        assertEquals(
            "zavolej mamince",
            IntentCatalog.normalizeUtterance("  ZAVOLEJ   mamince  "),
        )
        assertEquals("vytoč jana", IntentCatalog.normalizeUtterance("Vytoč Jana"))
    }

    @Test
    fun dsl_rejectsBlankIntentId() {
        assertFailsWith<IllegalArgumentException> {
            intentCatalog {
                intent("") {
                    phrase("x")
                }
            }
        }
    }

    @Test
    fun dsl_patternsVararg_buildsMultipleMatchers() {
        val cat =
            intentCatalog {
                intent("PAT", requiresConfirm = false) {
                    patterns("jedna {a}", "dva {a}")
                }
            }
        val engine = RuleBasedNluEngine(cat)
        val m1 = engine.blockingParse("jedna x") as NluResult.Matched
        val m2 = engine.blockingParse("dva yz") as NluResult.Matched
        assertEquals("PAT", m1.intent.intentId)
        assertEquals("x", m1.intent.slots["a"])
        assertEquals("yz", m2.intent.slots["a"])
    }

    @Test
    fun compiler_exactPhrase_noSlots() {
        val spec = PhraseTemplateCompiler.compile(" úplně   ztiš  ")
        assertTrue(spec.orderedSlotNames.isEmpty())
        assertNotNull(spec.regex.matchEntire("úplně ztiš"))
        assertNull(spec.regex.matchEntire("úplně ztiš x"))
    }

    @Test
    fun compiler_rejectsBlankNormalizedPhrase() {
        assertFailsWith<IllegalArgumentException> {
            PhraseTemplateCompiler.compile("   \t  ")
        }
    }

    @Test
    fun compiler_rejectsDuplicateSlotNames() {
        assertFailsWith<IllegalStateException> {
            PhraseTemplateCompiler.compile("{x} potom {x}")
        }
    }

    @Test
    fun compiler_twoSlots_firstLazyLastGreedy() {
        val spec = PhraseTemplateCompiler.compile("sms pro {contact} text {message}")
        assertEquals(listOf("contact", "message"), spec.orderedSlotNames)
        val m =
            spec.regex.matchEntire(
                "sms pro jan text ahoj tamní svět",
            )
        assertNotNull(m)
        assertEquals("jan", m.groupValues[1])
        assertEquals("ahoj tamní svět", m.groupValues[2])
    }
}
