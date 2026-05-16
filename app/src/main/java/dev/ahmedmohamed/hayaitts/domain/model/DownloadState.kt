package dev.ahmedmohamed.hayaitts.domain.model

/**
 * In-memory representation of one voice's download lifecycle. Persisted in
 * [dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity] as a
 * (status, progressBytes, totalBytes, errorMessage) tuple.
 */
sealed class DownloadState {
    data object Idle : DownloadState()
    data object Queued : DownloadState()
    data class Running(
        val pct: Float,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : DownloadState()
    data object Extracting : DownloadState()
    data object Done : DownloadState()
    data class Failed(val reason: String) : DownloadState()
    data object Cancelled : DownloadState()

    companion object {
        const val STATUS_IDLE = "idle"
        const val STATUS_QUEUED = "queued"
        const val STATUS_RUNNING = "running"
        const val STATUS_EXTRACTING = "extracting"
        const val STATUS_DONE = "done"
        const val STATUS_FAILED = "failed"
        const val STATUS_CANCELLED = "cancelled"
    }
}
