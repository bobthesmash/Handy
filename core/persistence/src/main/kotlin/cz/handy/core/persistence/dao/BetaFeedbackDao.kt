package cz.handy.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import cz.handy.core.persistence.entity.BetaFeedbackEntity

@Dao
interface BetaFeedbackDao {
    @Query("SELECT * FROM beta_feedback ORDER BY createdAtEpochMillis ASC")
    fun listAllOrdered(): List<BetaFeedbackEntity>

    @Query(
        """
        SELECT * FROM beta_feedback
        ORDER BY createdAtEpochMillis DESC
        LIMIT :limit
        """,
    )
    fun listRecentDescending(limit: Int): List<BetaFeedbackEntity>

    @Query("DELETE FROM beta_feedback")
    fun deleteAll()

    @Insert
    suspend fun insert(row: BetaFeedbackEntity): Long

    @Insert
    fun insertAll(rows: List<BetaFeedbackEntity>)

    @Transaction
    fun replaceAll(rows: List<BetaFeedbackEntity>) {
        deleteAll()
        val fresh = rows.map { it.copy(id = 0L) }
        if (fresh.isNotEmpty()) {
            insertAll(fresh)
        }
    }
}
