package dev.ahmedmohamed.hayaitts.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import dev.ahmedmohamed.hayaitts.data.db.entities.VoiceProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VoiceProfileDao {

    @Query("SELECT * FROM voice_profiles ORDER BY createdAt DESC")
    fun all(): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles WHERE voiceId = :voiceId ORDER BY createdAt DESC")
    fun forVoice(voiceId: String): Flow<List<VoiceProfileEntity>>

    @Query("SELECT * FROM voice_profiles WHERE voiceId = :voiceId AND isDefaultForVoice = 1 LIMIT 1")
    suspend fun defaultFor(voiceId: String): VoiceProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profile: VoiceProfileEntity): Long

    @Update
    suspend fun update(profile: VoiceProfileEntity)

    @Query("DELETE FROM voice_profiles WHERE id = :id")
    suspend fun delete(id: Long)

    /**
     * Marks one profile as default for its voice, clearing the flag on any
     * sibling rows in a single transaction.
     */
    @Transaction
    suspend fun setAsDefault(voiceId: String, profileId: Long) {
        clearDefaults(voiceId)
        applyDefault(profileId)
    }

    @Query("UPDATE voice_profiles SET isDefaultForVoice = 0 WHERE voiceId = :voiceId")
    suspend fun clearDefaults(voiceId: String)

    @Query("UPDATE voice_profiles SET isDefaultForVoice = 1 WHERE id = :profileId")
    suspend fun applyDefault(profileId: Long)
}
