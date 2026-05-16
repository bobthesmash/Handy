package cz.handy.feature.ui.backup

import android.content.Context
import cz.handy.core.persistence.HandyDatabase
import cz.handy.core.persistence.backup.HandyProfileBackup
import cz.handy.core.persistence.backup.HandyProfilePlainV1
import cz.handy.core.persistence.entity.BetaFeedbackEntity
import cz.handy.core.persistence.entity.ContactAliasEntity
import cz.handy.core.persistence.entity.EmbeddingVersionEntity
import cz.handy.feature.voiceid.storage.SpeakerEmbeddingEncryptedStore

/**
 * Export / import šifrovaného profilu (embedding + Room tabulky) — [F4-T03].
 */
class ProfileBackupCoordinator(
    context: Context,
) {
    private val app = context.applicationContext

    fun exportSealedPackage(password: CharArray): Result<ByteArray> =
        runCatching {
            val embeddingB64 = SpeakerEmbeddingEncryptedStore(app).exportEmbeddingBase64ForBackup()
            val db = HandyDatabase.getInstance(app)
            val aliases =
                db.contactAliasDao().listAll().map {
                    HandyProfilePlainV1.ProfileAliasEntryV1(
                        aliasKey = it.aliasKey,
                        targetContact = it.targetContact,
                        createdAtEpochMs = it.createdAtEpochMs,
                    )
                }
            val vers =
                db.embeddingVersionDao().listAllOrdered().map {
                    HandyProfilePlainV1.ProfileEmbeddingVersionEntryV1(
                        schemaVersion = it.schemaVersion,
                        modelLabel = it.modelLabel,
                        vectorDim = it.vectorDim,
                        savedAtEpochMs = it.savedAtEpochMs,
                        notes = it.notes,
                    )
                }
            val beta =
                db.betaFeedbackDao().listAllOrdered().map {
                    HandyProfilePlainV1.ProfileBetaFeedbackEntryV1(
                        createdAtEpochMs = it.createdAtEpochMillis,
                        satisfactionStars = it.satisfactionStars,
                        messageText = it.messageText,
                    )
                }
            val plain =
                HandyProfilePlainV1(
                    embeddingB64 = embeddingB64,
                    aliases = aliases,
                    embeddingVersions = vers,
                    betaFeedback = beta,
                )
            HandyProfileBackup.seal(plain, password)
        }

    fun importSealedPackage(
        bytes: ByteArray,
        password: CharArray,
    ): Result<Unit> =
        runCatching {
            val plain = HandyProfileBackup.open(bytes, password).getOrThrow()
            val aliasEntities =
                plain.aliases.map {
                    ContactAliasEntity(
                        aliasKey = it.aliasKey,
                        targetContact = it.targetContact,
                        createdAtEpochMs = it.createdAtEpochMs,
                    )
                }
            val verEntities =
                plain.embeddingVersions.map {
                    EmbeddingVersionEntity(
                        schemaVersion = it.schemaVersion,
                        modelLabel = it.modelLabel,
                        vectorDim = it.vectorDim,
                        savedAtEpochMs = it.savedAtEpochMs,
                        notes = it.notes,
                    )
                }
            val dbInstance = HandyDatabase.getInstance(app)
            dbInstance.restoreProfileSnapshot(aliasEntities, verEntities)
            plain.betaFeedback?.let { fb ->
                val rows =
                    fb.map {
                        BetaFeedbackEntity(
                            createdAtEpochMillis = it.createdAtEpochMs,
                            satisfactionStars = it.satisfactionStars,
                            messageText = it.messageText,
                        )
                    }
                dbInstance.betaFeedbackDao().replaceAll(rows)
            }
            val voice = SpeakerEmbeddingEncryptedStore(app)
            val embeddingB64 = plain.embeddingB64
            if (embeddingB64 != null) {
                voice.importEmbeddingFromBackupBase64(embeddingB64).getOrElse { throw it }
            } else {
                voice.clear()
            }
        }
}
