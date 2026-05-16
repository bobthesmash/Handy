package cz.handy.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cz.handy.core.persistence.entity.CommandLogEntity

@Dao
interface CommandLogDao {
    @Insert
    fun insert(row: CommandLogEntity): Long

    @Query("SELECT * FROM command_log ORDER BY created_at DESC LIMIT :limit")
    fun recent(limit: Int): List<CommandLogEntity>
}
