package cz.handy.core.persistence

import android.content.Context
import cz.handy.core.persistence.entity.ContactAliasEntity
import org.json.JSONObject

/**
 * Jednorázový import aliasů z JSON SharedPreferences ([F1-T17]) do Room ([F1-T18]).
 */
internal object LegacyContactAliasPrefs {
    private const val PREFS_NAME = "handy_contact_aliases"
    private const val KEY_JSON = "alias_map_v1"

    fun migrateIfNeeded(
        app: Context,
        db: HandyDatabase,
    ) {
        val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_JSON, null) ?: return
        val dao = db.contactAliasDao()
        db.runInTransaction {
            val obj = runCatching { JSONObject(json) }.getOrNull() ?: return@runInTransaction
            val keys = obj.keys()
            val now = System.currentTimeMillis()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = obj.optString(k, "").trim()
                if (k.isNotBlank() && v.isNotBlank()) {
                    dao.insert(
                        ContactAliasEntity(
                            aliasKey = k.lowercase(),
                            targetContact = v,
                            createdAtEpochMs = now,
                        ),
                    )
                }
            }
        }
        prefs.edit().remove(KEY_JSON).apply()
    }
}
