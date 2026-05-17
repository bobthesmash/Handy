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
            engine.blockingParse("vytoč účetnictví"),
        )
        assertIntent(
            "CALL",
            mapOf("contact" to "+420 123 456 789"),
            engine.blockingParse("zavolej číslo +420 123 456 789"),
        )
        assertIntent(
            "CALL",
            mapOf("contact" to "maminka"),
            engine.blockingParse("ZAVOLEJ   maminka"),
        )
    }

    @Test
    fun sms_bothTemplates() {
        assertIntent(
            "SEND_SMS",
            mapOf("contact" to "dědaček", "message" to "dorazím po obědě"),
            engine.blockingParse("pošli sms dědaček že dorazím po obědě"),
        )
        assertIntent(
            "SEND_SMS",
            mapOf("contact" to "ségře", "message" to "kup mléko"),
            engine.blockingParse("sms pro ségře text kup mléko"),
        )
    }

    @Test
    fun setAlarm_allTemplates() {
        assertIntent(
            "SET_ALARM",
            mapOf("time" to "úterý v osm"),
            engine.blockingParse("nastav budík na úterý v osm"),
        )
        assertIntent(
            "SET_ALARM",
            mapOf("time" to "sedm třicet"),
            engine.blockingParse("budík na sedm třicet"),
        )
        assertIntent(
            "SET_ALARM",
            mapOf("time" to "6:15", "label" to "fitko"),
            engine.blockingParse("budík čas 6:15 název fitko"),
        )
    }

    @Test
    fun metaCancelStopRepeat_phrases() {
        assertIntent("CANCEL", emptyMap(), engine.blockingParse("zruš to"))
        assertIntent("STOP", emptyMap(), engine.blockingParse("stop"))
        assertIntent("REPEAT", emptyMap(), engine.blockingParse("opakuj"))
        assertIntent("REPEAT", emptyMap(), engine.blockingParse("co jsi řekl"))
    }

    @Test
    fun volume_operationSlotVariants() {
        assertIntent(
            "VOLUME",
            mapOf("operation" to "sníž"),
            engine.blockingParse("sníž hlasitost"),
        )
        assertIntent(
            "VOLUME",
            mapOf("operation" to "ztiš"),
            engine.blockingParse("ztiš hlasitost"),
        )
    }

    @Test
    fun readLastNotification_bothPhrases() {
        assertIntent(
            "READ_LAST_NOTIFICATION",
            emptyMap(),
            engine.blockingParse("jaká je poslední notifikace"),
        )
    }

    @Test
    fun whatTimeDateBattery_phrases() {
        assertIntent("WHAT_TIME", emptyMap(), engine.blockingParse("kolik je hodin"))
        assertIntent("WHAT_TIME", emptyMap(), engine.blockingParse("jaký je čas"))
        assertIntent("WHAT_DATE", emptyMap(), engine.blockingParse("jaké je datum"))
        assertIntent("WHAT_BATTERY", emptyMap(), engine.blockingParse("jaká je baterka"))
    }

    @Test
    fun playMedia_phrases() {
        assertIntent("PLAY_MEDIA", emptyMap(), engine.blockingParse("přehraj hudbu"))
        assertIntent("PLAY_MEDIA", emptyMap(), engine.blockingParse("pusť hudbu"))
        assertIntent(
            "PLAY_MEDIA",
            mapOf("app" to "spotify"),
            engine.blockingParse("přehraj spotify"),
        )
        assertIntent(
            "PLAY_MEDIA",
            mapOf("app" to "youtube music"),
            engine.blockingParse("pusť youtube music"),
        )
    }

    @Test
    fun replyNotif_phrases() {
        assertIntent(
            "REPLY_NOTIF",
            mapOf("message" to "dorazím za půl hodiny"),
            engine.blockingParse("odpověz že dorazím za půl hodiny"),
        )
        assertIntent(
            "REPLY_NOTIF",
            mapOf("message" to "ok"),
            engine.blockingParse("napiš odpověď ok"),
        )
    }

    @Test
    fun openApp_phrases() {
        assertIntent(
            "OPEN_APP",
            mapOf("app" to "chrome"),
            engine.blockingParse("otevři chrome"),
        )
        assertIntent(
            "OPEN_APP",
            mapOf("app" to "nastavení"),
            engine.blockingParse("spusť nastavení"),
        )
        assertIntent(
            "OPEN_APP",
            mapOf("app" to "mapy"),
            engine.blockingParse("zapni aplikaci mapy"),
        )
    }

    @Test
    fun navigate_phrases() {
        assertIntent(
            "NAVIGATE",
            mapOf("place" to "hlavní nádraží praha"),
            engine.blockingParse("naviguj na hlavní nádraží praha"),
        )
        assertIntent(
            "NAVIGATE",
            mapOf("place" to "brno"),
            engine.blockingParse("trasa do brno"),
        )
    }

    @Test
    fun timer_phrases() {
        assertIntent(
            "TIMER",
            mapOf("duration" to "pět minut"),
            engine.blockingParse("časovač pět minut"),
        )
        assertIntent(
            "TIMER",
            mapOf("duration" to "30 sekund"),
            engine.blockingParse("timer 30 sekund"),
        )
        assertIntent(
            "TIMER",
            mapOf("duration" to "půl hodiny"),
            engine.blockingParse("odpočet půl hodiny"),
        )
    }

    @Test
    fun mediaCtrl_phrases() {
        assertIntent(
            "MEDIA_CTRL",
            mapOf("command" to "další"),
            engine.blockingParse("další hudba"),
        )
        assertIntent(
            "MEDIA_CTRL",
            mapOf("command" to "pauza"),
            engine.blockingParse("pauza hudba"),
        )
        assertIntent(
            "MEDIA_CTRL",
            mapOf("command" to "další"),
            engine.blockingParse("další skladba"),
        )
    }

    @Test
    fun contactAlias_phrases() {
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("target" to "petr vondrák", "alias" to "bratr"),
            engine.blockingParse("nazývej Petr Vondrák jako bratr"),
        )
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("target" to "máma", "alias" to "maminka"),
            engine.blockingParse("říkej máma jako maminka"),
        )
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("alias" to "babička", "target" to "helena novotná"),
            engine.blockingParse("ulož alias babička pro kontakt Helena Novotná"),
        )
        assertIntent(
            "SET_CONTACT_ALIAS",
            mapOf("target" to "+420 777 888 999", "alias" to "práce"),
            engine.blockingParse("kontakt +420 777 888 999 říkej práce"),
        )
        assertIntent(
            "REMOVE_CONTACT_ALIAS",
            mapOf("alias" to "bratr"),
            engine.blockingParse("smaž alias bratr"),
        )
        assertIntent(
            "REMOVE_CONTACT_ALIAS",
            mapOf("alias" to "starý alias"),
            engine.blockingParse("zapomeň alias starý alias"),
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
