package dev.ahmedmohamed.hayaitts.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadStateDao {
    @Query("SELECT * FROM download_states")
    fun getAll(): Flow<List<DownloadStateEntity>>

    @Query("SELECT * FROM download_states WHERE voiceId = :id LIMIT 1")
    suspend fun getById(id: String): DownloadStateEntity?

    @Upsert
    suspend fun upsert(entity: DownloadStateEntity)

    @Query("DELETE FROM download_states WHERE voiceId = :id")
    suspend fun deleteById(id: String)
}
