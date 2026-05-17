package dev.ahmedmohamed.hayaitts.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.ahmedmohamed.hayaitts.data.db.entities.PlaygroundSampleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaygroundSampleDao {
    @Query("SELECT * FROM playground_samples WHERE voiceId = :voiceId ORDER BY createdAt DESC")
    fun observeByVoice(voiceId: String): Flow<List<PlaygroundSampleEntity>>

    @Insert
    suspend fun insert(entity: PlaygroundSampleEntity): Long

    @Query("DELETE FROM playground_samples WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "SELECT * FROM playground_samples WHERE voiceId = :voiceId AND id NOT IN (" +
            "SELECT id FROM playground_samples WHERE voiceId = :voiceId ORDER BY createdAt DESC LIMIT :keep)",
    )
    suspend fun overflowFor(voiceId: String, keep: Int): List<PlaygroundSampleEntity>

    @Query(
        "DELETE FROM playground_samples WHERE voiceId = :voiceId AND id NOT IN (" +
            "SELECT id FROM playground_samples WHERE voiceId = :voiceId ORDER BY createdAt DESC LIMIT :keep)",
    )
    suspend fun trim(voiceId: String, keep: Int)
}
