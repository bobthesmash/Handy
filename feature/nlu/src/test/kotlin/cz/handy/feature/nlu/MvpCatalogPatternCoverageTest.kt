package cz.handy.feature.nlu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Pokrývá všechny fráze v [HandyNluCatalogs.mvp] a typické tvary slotů. */
class MvpCatalogPatternCoverageTest {
    private val engine = RuleBasedNluEngine(HandyNluCatalogs.mvp)

    @Test
    fun call_allTemplates() {
        assertIntent(
            "CALL",
            mapOf("contact" to "účetnictví"),
            engine.parse("vytoč účetnictví"),
        )
        assertIntent(
            "CALL",
            mapOf("contact" to "+420 123 456 789"),
            engine.parse("zavolej číslo +420 123 456 789"),
        )
        assertIntent(
            "CALL",
            mapOf("contact" to "maminka"),
            engine.parse("ZAVOLEJ   maminka"),
        )
    }

    @Test
    fun sms_bothTemplates() {
        assertIntent(
            "SEND_SMS",
            mapOf("contact" to "dědaček", "message" to "dorazím po obědě"),
            engine.parse("pošli sms dědaček že dorazím po obědě"),
        )
        assertIntent(
            "SEND_SMS",
            mapOf("contact" to "ségře", "message" to "kup mléko"),
            engine.parse("sms pro ségře text kup mléko"),
        )
    }

    @Test
    fun setAlarm_allTemplates() {
        assertIntent(
            "SET_ALARM",
            mapOf("time" to "úterý v osm"),
            engine.parse("nastav budík na úterý v osm"),
        )
        assertIntent(
            "SET_ALARM",
            mapOf("time" to "sedm třicet"),
            engine.parse("budík na sedm třicet"),
        )
        assertIntent(
            "SET_ALARM",
            mapOf("time" to "6:15", "label" to "fitko"),
            engine.parse("budík čas 6:15 název fitko"),
        )
    }

    @Test
    fun metaCancelStopRepeat_phrases() {
        assertIntent("CANCEL", emptyMap(), engine.parse("zruš to"))
        assertIntent("STOP", emptyMap(), engine.parse("stop"))
        assertIntent("REPEAT", emptyMap(), engine.parse("opakuj"))
        assertIntent("REPEAT", emptyMap(), engine.parse("co jsi řekl"))
    }

    @Test
    fun volume_operationSlotVariants() {
        assertIntent(
            "VOLUME",
            mapOf("operation" to "sníž"),
            engine.parse("sníž hlasitost"),
        )
        assertIntent(
            "VOLUME",
            mapOf("operation" to "ztiš"),
            engine.parse("ztiš hlasitost"),
        )
    }

    @Test
    fun readLastNotification_bothPhrases() {
        assertIntent(
            "READ_LAST_NOTIFICATION",
            emptyMap(),
            engine.parse("jaká je poslední notifikace"),
        )
    }

    @Test
    fun whatTimeDateBattery_phrases() {
        assertIntent("WHAT_TIME", emptyMap(), engine.parse("kolik je hodin"))
        assertIntent("WHAT_TIME", emptyMap(), engine.parse("jaký je čas"))
        assertIntent("WHAT_DATE", emptyMap(), engine.parse("jaké je datum"))
        assertIntent("WHAT_BATTERY", emptyMap(), engine.parse("jaká je baterka"))
    }

    @Test
    fun playMedia_phrases() {
        assertIntent("PLAY_MEDIA", emptyMap(), engine.parse("přehraj hudbu"))
        assertIntent("PLAY_MEDIA", emptyMap(), engine.parse("pusť hudbu"))
        assertIntent(
            "PLAY_MEDIA",
            mapOf("app" to "spotify"),
            engine.parse("přehraj spotify"),
        )
        assertIntent(
            "PLAY_MEDIA",
            mapOf("app" to "youtube music"),
            engine.parse("pusť youtube music"),
        )
    }

    @Test
    fun replyNotif_phrases() {
        assertIntent(
            "REPLY_NOTIF",
            mapOf("message" to "dorazím za půl hodiny"),
            engine.parse("odpověz že dorazím za půl hodiny"),
        )
        assertIntent(
            "REPLY_NOTIF",
            mapOf("message" to "ok"),
            engine.parse("napiš odpověď ok"),
        )
    }

    @Test
    fun openApp_phrases() {
        assertIntent(
            "OPEN_APP",
            mapOf("app" to "chrome"),
            engine.parse("otevři chrome"),
        )
        assertIntent(
            "OPEN_APP",
            mapOf("app" to "nastavení"),
            engine.parse("spusť nastavení"),
        )
        assertIntent(
            "OPEN_APP",
            mapOf("app" to "mapy"),
            engine.parse("zapni aplikaci mapy"),
        )
    }

    @Test
    fun navigate_phrases() {
        assertIntent(
            "NAVIGATE",
            mapOf("place" to "hlavní nádraží praha"),
            engine.parse("naviguj na hlavní nádraží praha"),
        )
        assertIntent(
            "NAVIGATE",
            mapOf("place" to "brno"),
            engine.parse("trasa do brno"),
        )
    }

    @Test
    fun timer_phrases() {
        assertIntent(
            "TIMER",
            mapOf("duration" to "pět minut"),
            engine.parse("časovač pět minut"),
        )
        assertIntent(
            "TIMER",
            mapOf("duration" to "30 sekund"),
            engine.parse("timer 30 sekund"),
        )
        assertIntent(
            "TIMER",
            mapOf("duration" to "půl hodiny"),
            engine.parse("odpočet půl hodiny"),
        )
    }

    @Test
    fun mediaCtrl_phrases() {
        assertIntent(
            "MEDIA_CTRL",
            mapOf("command" to "další"),
            engine.parse("další hudba"),
        )
        assertIntent(
            "MEDIA_CTRL",
            mapOf("command" to "pauza"),
            engine.parse("pauza hudba"),
        )
        assertIntent(
            "MEDIA_CTRL",
            mapOf("command" to "další"),
            engine.parse("další skladba"),
        )
    }

    @Test
    fun contactAlias_phrases() {
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("target" to "petr vondrák", "alias" to "bratr"),
            engine.parse("nazývej Petr Vondrák jako bratr"),
        )
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("target" to "máma", "alias" to "maminka"),
            engine.parse("říkej máma jako maminka"),
        )
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("alias" to "babička", "target" to "helena novotná"),
            engine.parse("ulož alias babička pro kontakt Helena Novotná"),
        )
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("target" to "+420 777 888 999", "alias" to "práce"),
            engine.parse("kontakt +420 777 888 999 říkej práce"),
        )
        assertIntent(
            "REMOVE_CONTACT_ALIAS",
            mapOf("alias" to "bratr"),
            engine.parse("smaž alias bratr"),
        )
        assertIntent(
            "REMOVE_CONTACT_ALIAS",
            mapOf("alias" to "starý alias"),
            engine.parse("zapomeň alias starý alias"),
        )
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
