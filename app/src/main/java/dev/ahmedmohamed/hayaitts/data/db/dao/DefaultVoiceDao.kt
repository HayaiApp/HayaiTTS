package dev.ahmedmohamed.hayaitts.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.ahmedmohamed.hayaitts.data.db.entities.DefaultVoiceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DefaultVoiceDao {
    @Query("SELECT * FROM default_voices")
    fun getAll(): Flow<List<DefaultVoiceEntity>>

    @Upsert
    suspend fun upsert(entity: DefaultVoiceEntity)

    @Query("DELETE FROM default_voices WHERE locale = :locale")
    suspend fun deleteByLocale(locale: String)
}
