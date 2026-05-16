package cz.handy.core.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cz.handy.core.persistence.entity.ContactAliasEntity

@Dao
interface ContactAliasDao {
    @Query("SELECT target_contact FROM contacts_aliases WHERE alias_key = :key LIMIT 1")
    fun findTarget(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: ContactAliasEntity): Long

    @Query("DELETE FROM contacts_aliases WHERE alias_key = :key")
    fun deleteByKey(key: String): Int

    @Query("SELECT * FROM contacts_aliases ORDER BY alias_key ASC")
    fun listAll(): List<ContactAliasEntity>

    @Query("DELETE FROM contacts_aliases")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM contacts_aliases")
    fun count(): Int
}
