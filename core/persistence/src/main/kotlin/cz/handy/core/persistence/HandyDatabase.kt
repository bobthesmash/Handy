package cz.handy.core.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cz.handy.core.persistence.dao.BetaFeedbackDao
import cz.handy.core.persistence.dao.CommandLogDao
import cz.handy.core.persistence.dao.ContactAliasDao
import cz.handy.core.persistence.dao.EmbeddingVersionDao
import cz.handy.core.persistence.entity.BetaFeedbackEntity
import cz.handy.core.persistence.entity.CommandLogEntity
import cz.handy.core.persistence.entity.ContactAliasEntity
import cz.handy.core.persistence.entity.EmbeddingVersionEntity

@Database(
    entities = [
        ContactAliasEntity::class,
        CommandLogEntity::class,
        EmbeddingVersionEntity::class,
        BetaFeedbackEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class HandyDatabase : RoomDatabase() {
    abstract fun contactAliasDao(): ContactAliasDao

    abstract fun commandLogDao(): CommandLogDao

    abstract fun embeddingVersionDao(): EmbeddingVersionDao

    abstract fun betaFeedbackDao(): BetaFeedbackDao

    /** Nahradí aliasy a záznamy verzí embeddingu atomicky ([F4-T03]). */
    @Transaction
    open fun restoreProfileSnapshot(
        aliases: List<ContactAliasEntity>,
        embeddingVersions: List<EmbeddingVersionEntity>,
    ) {
        contactAliasDao().deleteAll()
        embeddingVersionDao().deleteAll()
        aliases.forEach { contactAliasDao().insert(it) }
        embeddingVersions.forEach { embeddingVersionDao().insert(it) }
    }

    companion object {
        internal val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `beta_feedback` (
                          `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                          `createdAtEpochMillis` INTEGER NOT NULL,
                          `satisfactionStars` INTEGER NOT NULL,
                          `messageText` TEXT NOT NULL
                        )
                        """.trimIndent(),
                    )
                }
            }

        private const val DB_NAME = "handy.db"

        @Volatile
        private var instance: HandyDatabase? = null

        fun getInstance(context: Context): HandyDatabase {
            val app = context.applicationContext
            return instance
                ?: synchronized(this) {
                    instance
                        ?: Room
                            .databaseBuilder(app, HandyDatabase::class.java, DB_NAME)
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_1_2)
                            .build()
                            .also { created ->
                                LegacyContactAliasPrefs.migrateIfNeeded(app, created)
                                instance = created
                            }
                }
        }
    }
}
