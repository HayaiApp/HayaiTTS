package dev.ahmedmohamed.hayaitts.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.ahmedmohamed.hayaitts.data.db.entities.PronunciationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PronunciationDao {

    @Query("SELECT * FROM pronunciations ORDER BY locale, word")
    fun all(): Flow<List<PronunciationEntity>>

    @Query("SELECT * FROM pronunciations WHERE locale = :locale")
    suspend fun forLocale(locale: String): List<PronunciationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PronunciationEntity)

    @Delete
    suspend fun delete(entry: PronunciationEntity)

    @Query("DELETE FROM pronunciations WHERE locale = :locale")
    suspend fun clearLocale(locale: String)
}
