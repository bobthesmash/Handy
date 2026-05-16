package cz.handy.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import cz.handy.core.persistence.entity.EmbeddingVersionEntity

@Dao
interface EmbeddingVersionDao {
    @Query("SELECT * FROM embedding_versions ORDER BY saved_at ASC")
    fun listAllOrdered(): List<EmbeddingVersionEntity>

    @Query("DELETE FROM embedding_versions")
    fun deleteAll()

    @Insert
    fun insert(row: EmbeddingVersionEntity): Long

    @Query("SELECT * FROM embedding_versions ORDER BY saved_at DESC LIMIT 1")
    fun latest(): EmbeddingVersionEntity?
}
