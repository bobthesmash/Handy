package cz.handy.core.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "embedding_versions")
data class EmbeddingVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "schema_version") val schemaVersion: Int,
    @ColumnInfo(name = "model_label") val modelLabel: String,
    @ColumnInfo(name = "vector_dim") val vectorDim: Int,
    @ColumnInfo(name = "saved_at") val savedAtEpochMs: Long,
    @ColumnInfo(name = "notes") val notes: String?,
)
