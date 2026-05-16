package cz.handy.core.persistence

import android.content.Context
import cz.handy.core.persistence.entity.ContactAliasEntity

/**
 * Kosinus / slot rozšíření bez DB — sdílené s testy ([F1-T17]/[F1-T18]).
 */
internal object ContactAliasExpansion {
    fun expand(
        lowercaseKeyToTarget: Map<String, String>,
        contactSlotRaw: String,
    ): String {
        val trimmed = contactSlotRaw.trim()
        if (trimmed.isEmpty()) return contactSlotRaw
        val repl = lowercaseKeyToTarget[trimmed.lowercase()] ?: return contactSlotRaw
        val next = repl.trim()
        return if (next.isEmpty()) contactSlotRaw else next
    }
}

/**
 * Alias pro NLU slot **`contact`** — úložiště Room `contacts_aliases` ([F1-T18]).
 */
class ContactAliasStore(
    context: Context,
) {
    private val db = HandyDatabase.getInstance(context.applicationContext)
    private val dao = db.contactAliasDao()

    fun expandForDial(contactSlotRaw: String): String {
        val trimmed = contactSlotRaw.trim()
        if (trimmed.isEmpty()) return contactSlotRaw
        val target = dao.findTarget(trimmed.lowercase()) ?: return contactSlotRaw
        val t = target.trim()
        return if (t.isEmpty()) contactSlotRaw else t
    }

    fun upsert(
        aliasSpoken: String,
        contactQuery: String,
    ): Result<Unit> {
        val key = aliasSpoken.trim().lowercase()
        val value = contactQuery.trim()
        if (key.isEmpty() || value.isEmpty()) {
            return Result.failure(
                IllegalArgumentException("Alias i cíl (jméno/číslo) musí být neprázdné."),
            )
        }
        if (dao.findTarget(key) == null && dao.count() >= MAX_CONTACT_ALIASES) {
            return Result.failure(
                IllegalStateException("Maximálně $MAX_CONTACT_ALIASES aliasů — smaž staré."),
            )
        }
        dao.insert(
            ContactAliasEntity(
                aliasKey = key,
                targetContact = value,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        return Result.success(Unit)
    }

    fun remove(aliasSpoken: String) {
        val key = aliasSpoken.trim().lowercase()
        if (key.isEmpty()) return
        dao.deleteByKey(key)
    }

    fun allAliasesSorted(): List<Pair<String, String>> = dao.listAll().map { it.aliasKey to it.targetContact }

    companion object {
        const val MAX_CONTACT_ALIASES = 48
    }
}
