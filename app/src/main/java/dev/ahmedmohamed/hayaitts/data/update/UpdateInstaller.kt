package dev.ahmedmohamed.hayaitts.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

/**
 * Downloads the universal APK from the chosen GitHub release asset and hands it
 * off to the system package installer.
 *
 * The download stream is exposed as a `Flow<DownloadProgress>` so the dialog
 * can show a `LinearWavyProgressIndicator` keyed off [DownloadProgress.Running.fraction].
 * Cancellation cooperates with the producer: collecting `awaitClose` cancels the
 * OkHttp `Call`, deletes the partial file, and emits [DownloadProgress.Cancelled].
 *
 * Storage layout: APKs land under `cacheDir/updates/<tag>.apk`. The path is
 * intentionally inside `cacheDir` so the OS can reclaim space if the user
 * abandons the install; the `FileProvider` declared in the manifest exposes
 * this directory under the authority `${applicationId}.updates`.
 */
class UpdateInstaller(
    private val context: Context,
    private val okHttp: OkHttpClient,
) {
    private val log = Logger.withTag("UpdateInstaller")

    /** Stream the chosen release asset. The caller cancels by cancelling the collecting coroutine. */
    fun download(url: String, tag: String): Flow<DownloadProgress> = callbackFlow {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        // Guard against tag-name path traversal — the GitHub API hands us tags
        // like `v1.2.3-b1` but we sanitize defensively.
        val safeTag = tag.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = File(dir, "$safeTag.apk")
        if (target.exists()) target.delete()

        val request = Request.Builder().url(url).build()
        val call: Call = okHttp.newCall(request)

        // Kick off the network IO on the OkHttp dispatcher thread; we'll
        // bridge progress back via trySend below.
        val thread = Thread {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        trySend(DownloadProgress.Failed("HTTP ${response.code}"))
                        close()
                        return@Thread
                    }
                    val total = response.body?.contentLength() ?: -1L
                    val source = response.body?.byteStream()
                        ?: run {
                            trySend(DownloadProgress.Failed("Empty body"))
                            close()
                            return@Thread
                        }
                    var downloaded = 0L
                    var lastEmit = 0L
                    FileOutputStream(target).use { sink ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            if (call.isCanceled()) {
                                target.delete()
                                trySend(DownloadProgress.Cancelled)
                                close()
                                return@Thread
                            }
                            val read = source.read(buf)
                            if (read <= 0) break
                            sink.write(buf, 0, read)
                            downloaded += read
                            val now = System.currentTimeMillis()
                            // 100 ms is enough to drive a 60 fps progress bar
                            // without flooding the channel on fast Wi-Fi.
                            if (now - lastEmit >= 100L) {
                                lastEmit = now
                                trySend(DownloadProgress.Running(downloaded, total))
                            }
                        }
                    }
                    trySend(DownloadProgress.Running(downloaded, total))
                    trySend(DownloadProgress.Done(target))
                    close()
                }
            } catch (t: Throwable) {
                log.w(t) { "APK download failed" }
                target.delete()
                trySend(DownloadProgress.Failed(t.message ?: t::class.simpleName.orEmpty()))
                close()
            }
        }
        thread.start()

        awaitClose {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Hand the downloaded APK to the system package installer.
     *
     * Uses [Intent.ACTION_VIEW] with the `application/vnd.android.package-archive`
     * MIME type plus `FLAG_GRANT_READ_URI_PERMISSION` so the installer activity
     * (signed by Google, separate UID) can read the file we exposed through our
     * [FileProvider]. On API 26+ the manifest's `REQUEST_INSTALL_PACKAGES`
     * permission is what lets us launch this intent without an OS error toast.
     */
    fun install(apk: File) {
        val authority = "${context.packageName}.updates"
        val uri: Uri = FileProvider.getUriForFile(context, authority, apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        context.startActivity(intent)
    }
}
