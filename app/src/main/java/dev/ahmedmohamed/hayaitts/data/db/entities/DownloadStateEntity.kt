package dev.ahmedmohamed.hayaitts.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persists the most recent state of one voice's download. We keep this in
 * Room (rather than only in WorkManager) so cancelled / failed downloads
 * survive process death and so we can render UI without waiting on the
 * WorkManager observer.
 *
 * [status] is one of the `STATUS_*` constants on
 * [dev.ahmedmohamed.hayaitts.domain.model.DownloadState.Companion].
 */
@Entity(tableName = "download_states")
data class DownloadStateEntity(
    @PrimaryKey val voiceId: String,
    val status: String,
    val progressBytes: Long,
    val totalBytes: Long,
    val errorMessage: String?,
    val updatedAt: Long,
)
