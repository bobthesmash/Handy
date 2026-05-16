package cz.handy.core.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_log")
data class CommandLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "intent_id") val intentId: String,
    @ColumnInfo(name = "slots_json") val slotsJson: String,
    @ColumnInfo(name = "success") val success: Boolean,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "created_at") val createdAtEpochMs: Long,
)
