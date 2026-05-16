package cz.handy.core.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "contacts_aliases",
    indices = [Index(value = ["alias_key"], unique = true)],
)
data class ContactAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "alias_key") val aliasKey: String,
    @ColumnInfo(name = "target_contact") val targetContact: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMs: Long,
)
