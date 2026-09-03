package cz.handy.feature.nlu

import cz.handy.feature.nlu.internal.IntentDefinition
import cz.handy.feature.nlu.internal.PhraseTemplateCompiler
import java.util.Locale

/** Neměnný katalog definovaný přes [intentCatalog]. */
class IntentCatalog internal constructor(
    internal val intents: List<IntentDefinition>,
) {
    val intentIds: List<String>
        get() = intents.map { it.id }

    /** Pro validaci strukturovaného LLM výstupu ([F5-T01]). */
    fun requiresConfirmFor(intentId: String): Boolean = intents.find { it.id == intentId }?.requiresConfirm ?: false

    /** Vrací `false`, pokud [intentId] není v katalogu nebo sloty neprošly [IntentDefinition.slotOk]. */
    fun slotsSatisfied(
        intentId: String,
        slots: Map<String, String>,
    ): Boolean {
        val def = intents.find { it.id == intentId } ?: return false
        return def.slotOk(slots)
    }

    init {
        val dup =
            intents
                .groupingBy { it.id }
                .eachCount()
                .filter { it.value > 1 }
                .keys
        require(dup.isEmpty()) {
            "Duplicitní intent id ve stejném katalogu: $dup"
        }
    }

    companion object {
        private val CS = Locale.forLanguageTag("cs-CZ")

        internal fun normalizeUtterance(utterance: String): String = utterance.trim().lowercase(CS).replace(Regex("\\s+"), " ")
    }
}

class IntentCatalogBuilder {
    private val definitions = mutableListOf<IntentDefinition>()

    fun intent(
        id: String,
        requiresConfirm: Boolean = false,
        body: IntentSpec.() -> Unit,
    ) {
        require(id.isNotBlank()) { "Intent id nesmí být prázdný." }
        definitions +=
            IntentSpec(
                id = id,
                requiresConfirm = requiresConfirm,
            ).apply(body)
                .compile()
    }

    fun build(): IntentCatalog = IntentCatalog(definitions.toList())
}

fun intentCatalog(init: IntentCatalogBuilder.() -> Unit): IntentCatalog = IntentCatalogBuilder().apply(init).build()

private val SPEC_LOCALE = Locale.forLanguageTag("cs-CZ")

class IntentSpec internal constructor(
    private val id: String,
    private val requiresConfirm: Boolean,
) {
    private val templates = mutableListOf<String>()
    private val declaredSlots = mutableMapOf<String, Boolean>()

    fun patterns(vararg templates: String) {
        templates.forEach { phrase(it) }
    }

    fun phrase(template: String) {
        templates += template
    }

    fun slot(
        name: String,
        configure: SlotBuilder.() -> Unit = {},
    ) {
        val key = name.trim().lowercase(SPEC_LOCALE)
        require(key.isNotBlank()) { "Název slotu nesmí být prázdný." }
        val b = SlotBuilder().apply(configure)
        declaredSlots[key] = b.required
    }

    internal fun compile(): IntentDefinition {
        require(templates.isNotEmpty()) {
            "Intent $id nemá žádné patterns / phrase — přidej aspoň jednu šablonu."
        }
        val matchers = templates.map { PhraseTemplateCompiler.compile(it) }
        val discovered =
            matchers
                .flatMap { it.orderedSlotNames + it.staticSlots.keys }
                .toSet()

        val slotRequired = LinkedHashMap<String, Boolean>()
        discovered.forEach { slotRequired[it] = true }

        for ((slotName, required) in declaredSlots) {
            require(slotName in discovered) {
                "Intent $id: slot \"$slotName\" se neobjevuje v žádné šabloně."
            }
            slotRequired[slotName] = required
        }

        return IntentDefinition(
            id = id,
            requiresConfirm = requiresConfirm,
            matchers = matchers,
            slotRequired = slotRequired.toMap(),
        )
    }
}

class SlotBuilder {
    /** `false` = slot může být vynechaný/prázdný (např. volitelný doplněk později). */
    var required: Boolean = true
}
