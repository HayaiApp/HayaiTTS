package dev.ahmedmohamed.hayaitts.data.update

/**
 * Streaming progress emitted by [UpdateInstaller.download].
 *
 * [bytesRead]/[totalBytes] are reported in raw bytes so the UI can render both
 * the wavy progress fraction and a "12 MB / 175 MB" caption without re-doing the
 * arithmetic. [totalBytes] is `-1` when the server omits Content-Length — the UI
 * falls back to indeterminate in that case.
 */
sealed interface DownloadProgress {
    data class Running(val bytesRead: Long, val totalBytes: Long) : DownloadProgress {
        val fraction: Float
            get() = if (totalBytes > 0) (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
    }

    data class Done(val apkFile: java.io.File) : DownloadProgress

    data class Failed(val reason: String) : DownloadProgress

    data object Cancelled : DownloadProgress
}
