package cz.handy.feature.nlu

import cz.handy.core.common.asr.PlaceAsrLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class NavigatePlaceSlotBinderTest {
    @Test
    fun czech_location_replaces_place_with_location_asr_tail() {
        val parsed =
            ParsedIntent(
                intentId = "NAVIGATE",
                slots = mapOf("place" to "brno"),
                requiresConfirm = false,
            )
        val out =
            NavigatePlaceSlotBinder.bind(
                parsed,
                PlaceAsrLanguage.CZECH,
                "Karlův most",
            )
        assertEquals("NAVIGATE", out.intentId)
        assertEquals("Karlův most", out.slots["place"])
    }

    @Test
    fun english_location_keeps_command_asr_place() {
        val parsed =
            ParsedIntent(
                intentId = "NAVIGATE",
                slots = mapOf("place" to "austin"),
                requiresConfirm = false,
            )
        val out =
            NavigatePlaceSlotBinder.bind(
                parsed,
                PlaceAsrLanguage.ENGLISH,
                "Karlův most",
            )
        assertEquals("austin", out.slots["place"])
    }

    @Test
    fun blank_czech_tail_keeps_english_place() {
        val parsed =
            ParsedIntent(
                intentId = "NAVIGATE",
                slots = mapOf("place" to "plzen"),
                requiresConfirm = false,
            )
        val out = NavigatePlaceSlotBinder.bind(parsed, PlaceAsrLanguage.CZECH, "  ")
        assertEquals("plzen", out.slots["place"])
    }

    @Test
    fun non_navigate_intents_are_unchanged() {
        val parsed =
            ParsedIntent(
                intentId = "CALL",
                slots = mapOf("contact" to "jan"),
                requiresConfirm = true,
            )
        val out = NavigatePlaceSlotBinder.bind(parsed, PlaceAsrLanguage.CZECH, "praha")
        assertEquals("jan", out.slots["contact"])
        assertEquals("CALL", out.intentId)
    }
}
