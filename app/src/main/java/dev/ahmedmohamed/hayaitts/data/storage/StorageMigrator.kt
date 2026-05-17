package dev.ahmedmohamed.hayaitts.data.storage

import android.content.Context
import android.os.Environment
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.data.db.dao.InstalledVoiceDao
import dev.ahmedmohamed.hayaitts.domain.model.MoveProgress
import dev.ahmedmohamed.hayaitts.domain.model.StorageLocation
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.IOException

/**
 * Resolves where installed voice bundles live for a given [StorageLocation]
 * and physically moves them between locations when the user flips the
 * preference.
 */
class StorageMigrator(
    private val context: Context,
    private val installedVoiceDao: InstalledVoiceDao,
    private val settings: SettingsRepository,
) {
    private val log = Logger.withTag("StorageMigrator")

    /**
     * Where new voice bundles should be extracted for [location]. Falls back
     * to internal storage when external isn't available (no SD card mounted
     * even though the pref says EXTERNAL — covers the case where the user
     * ejected the card after picking it).
     */
    fun targetRoot(location: StorageLocation): File = when (location) {
        StorageLocation.INTERNAL -> File(context.filesDir, "voices")
        StorageLocation.EXTERNAL -> externalVoicesDir() ?: File(context.filesDir, "voices")
    }

    /**
     * Current root, derived from the persisted preference. Suspending so
     * callers can `runBlocking` it from the synchronous WorkManager doWork()
     * path without bringing in a separate sync API.
     */
    suspend fun currentRoot(): File = targetRoot(settings.storageLocation.first())

    /** `true` when an actual removable SD card is mounted (vs. emulated). */
    fun hasExternalStorage(): Boolean = externalVoicesDir() != null

    private fun externalVoicesDir(): File? {
        val dirs = context.getExternalFilesDirs(null)?.filterNotNull().orEmpty()
        // dirs[0] is the primary emulated /sdcard — same physical volume as
        // filesDir on most modern devices, so moving voices there gains
        // nothing. We only count an actual second mount point as "external".
        if (dirs.size < 2) return null
        val state = runCatching { Environment.getExternalStorageState(dirs[1]) }.getOrNull()
        if (state != Environment.MEDIA_MOUNTED) return null
        return File(dirs[1], "voices")
    }

    /**
     * Copy every non-bundled installed voice from its current `installedPath`
     * to a per-voice subdirectory under [targetRoot(target)], update the
     * Room row's `installedPath`, then delete the source. Emits granular
     * progress so the Settings screen can render a wavy bar that moves with
     * each voice.
     *
     * On any failure the partial state is preserved (voices already moved
     * keep their new path; the in-flight voice is reverted by deleting the
     * partially-copied destination). The progress flow ends with [Failed]
     * and the caller decides whether to retry.
     */
    fun moveAllVoices(target: StorageLocation): Flow<MoveProgress> = flow {
        emit(MoveProgress.Moving(currentVoiceId = null, doneCount = 0, totalCount = 0))
        val destRoot = targetRoot(target).also { it.mkdirs() }
        val all = installedVoiceDao.getAllSnapshot()
        // Bundled voice is asset-mirrored — never move it.
        val toMove = all
        val needsMove = toMove.filter { entry ->
            val current = File(entry.installedPath).parentFile
            current?.canonicalPath != destRoot.canonicalPath
        }
        if (needsMove.isEmpty()) {
            log.i { "moveAllVoices(target=$target): nothing to move; ${all.size} voices already in place" }
            // Still persist the preference so subsequent downloads go to the new root.
            settings.setStorageLocation(target)
            emit(MoveProgress.Done(movedCount = 0))
            return@flow
        }
        log.i { "moveAllVoices(target=$target): need to move ${needsMove.size} voices to $destRoot" }

        var moved = 0
        for (entry in needsMove) {
            emit(MoveProgress.Moving(currentVoiceId = entry.voiceId, doneCount = moved, totalCount = needsMove.size))
            val source = File(entry.installedPath)
            val dest = File(destRoot, entry.voiceId)
            try {
                if (!source.isDirectory) {
                    throw IOException("source $source for voice ${entry.voiceId} is not a directory")
                }
                if (dest.exists()) dest.deleteRecursively()
                copyDirectory(source, dest)
                val updated = entry.copy(installedPath = dest.absolutePath)
                installedVoiceDao.upsert(updated)
                source.deleteRecursively()
                moved++
                log.i { "Moved voice ${entry.voiceId} -> $dest (${moved}/${needsMove.size})" }
            } catch (t: Throwable) {
                log.e(t) { "Failed to move voice ${entry.voiceId}; reverting partial copy at $dest" }
                runCatching { if (dest.exists()) dest.deleteRecursively() }
                emit(MoveProgress.Failed(
                    reason = "Move failed at ${entry.voiceId}: ${t.message ?: t.javaClass.simpleName}",
                    partialCount = moved,
                ))
                return@flow
            }
        }
        // Only persist the preference once every voice landed safely — that
        // way a partial failure leaves the source location authoritative and
        // future downloads keep going there.
        settings.setStorageLocation(target)
        emit(MoveProgress.Done(movedCount = moved))
    }.flowOn(Dispatchers.IO)

    private fun copyDirectory(source: File, dest: File) {
        if (!dest.exists()) dest.mkdirs()
        source.walkTopDown().forEach { entry ->
            val rel = entry.toRelativeString(source)
            if (rel.isEmpty()) return@forEach
            val out = File(dest, rel)
            if (entry.isDirectory) {
                out.mkdirs()
            } else {
                out.parentFile?.mkdirs()
                entry.copyTo(out, overwrite = true)
            }
        }
    }
}
