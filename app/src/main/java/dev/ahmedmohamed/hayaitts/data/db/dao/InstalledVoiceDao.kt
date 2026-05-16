package dev.ahmedmohamed.hayaitts.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.ahmedmohamed.hayaitts.data.db.entities.InstalledVoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstalledVoiceDao {
    @Query("SELECT * FROM installed_voices ORDER BY installedAt DESC")
    fun getAll(): Flow<List<InstalledVoiceEntity>>

    @Query("SELECT * FROM installed_voices")
    suspend fun getAllSnapshot(): List<InstalledVoiceEntity>

    @Query("SELECT * FROM installed_voices WHERE voiceId = :id LIMIT 1")
    suspend fun getById(id: String): InstalledVoiceEntity?

    @Upsert
    suspend fun upsert(entity: InstalledVoiceEntity)

    @Query("DELETE FROM installed_voices WHERE voiceId = :id")
    suspend fun deleteById(id: String)
}
