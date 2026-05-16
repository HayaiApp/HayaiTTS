package dev.ahmedmohamed.hayaitts.tts

import android.content.Context
import android.content.res.AssetManager
import co.touchlab.kermit.Logger
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.io.FileOutputStream

/**
 * Thin wrapper around sherpa-onnx's [OfflineTts] for the bundled Piper Amy
 * voice. Loads the voice on first use, then keeps the instance alive for the
 * lifetime of the process.
 *
 * The native code in sherpa-onnx needs real filesystem paths (open() / fopen())
 * and cannot read from Android's compressed asset stream, so the asset tree is
 * mirrored into [Context.filesDir] on first use. A tiny manifest sidecar is
 * dropped next to the mirror so subsequent launches can skip the copy with a
 * cheap file-count + total-bytes check.
 */
class SherpaTtsRuntime private constructor(private val tts: OfflineTts) {

    val sampleRate: Int get() = tts.sampleRate()

    data class SynthesisOutput(val sampleRate: Int, val samples: FloatArray)

    fun synthesize(text: String, sid: Int = 0, speed: Float = 1.0f): SynthesisOutput {
        val audio = tts.generate(text = text, sid = sid, speed = speed)
        return SynthesisOutput(sampleRate = audio.sampleRate, samples = audio.samples)
    }

    companion object {
        const val VOICE_ASSET_SUBDIR = "voices/en_US-amy-low"
        private const val MODEL_FILE = "model.onnx"
        private const val TOKENS_FILE = "tokens.txt"
        private const val ESPEAK_DIR = "espeak-ng-data"
        private const val MANIFEST_FILE = ".mirror-manifest"

        private val log = Logger.withTag("SherpaTtsRuntime")

        @Volatile
        private var instance: SherpaTtsRuntime? = null

        fun get(context: Context): SherpaTtsRuntime {
            instance?.let { return it }
            return synchronized(this) {
                instance ?: build(context.applicationContext).also { instance = it }
            }
        }

        private fun build(context: Context): SherpaTtsRuntime {
            val voiceDir = File(context.filesDir, VOICE_ASSET_SUBDIR)
            mirrorAssetTreeIfNeeded(context, VOICE_ASSET_SUBDIR, voiceDir)

            val modelPath = File(voiceDir, MODEL_FILE).absolutePath
            val tokensPath = File(voiceDir, TOKENS_FILE).absolutePath
            val dataDirPath = File(voiceDir, ESPEAK_DIR).absolutePath

            log.i { "Loading Piper Amy voice from $voiceDir" }
            val config = OfflineTtsConfig(
                model = OfflineTtsModelConfig(
                    vits = OfflineTtsVitsModelConfig(
                        model = modelPath,
                        tokens = tokensPath,
                        dataDir = dataDirPath,
                        lengthScale = 1.0f,
                    ),
                    numThreads = 1,
                    debug = false,
                ),
                maxNumSentences = 2,
            )
            val tts = OfflineTts(assetManager = null, config = config)
            log.i { "OfflineTts ready (sampleRate=${tts.sampleRate()})" }
            return SherpaTtsRuntime(tts)
        }

        /**
         * Mirror [assetSubdir] from the APK assets into [destRoot] if the
         * destination is missing or the manifest sidecar disagrees with the
         * mirrored tree.
         *
         * Fast path: if the manifest sidecar exists and reports the same file
         * count + total on-disk bytes as the current mirror, we skip the copy
         * without touching the assets at all.
         */
        private fun mirrorAssetTreeIfNeeded(
            context: Context,
            assetSubdir: String,
            destRoot: File,
        ) {
            val manifestFile = File(destRoot, MANIFEST_FILE)
            if (destRoot.isDirectory && manifestFile.isFile) {
                val cached = manifestFile.readText().trim()
                val actual = manifestOnDisk(destRoot, manifestFile)
                if (cached == actual) {
                    log.i { "Voice asset mirror up to date at $destRoot" }
                    return
                }
                log.i { "Voice asset mirror stale (manifest=$cached, disk=$actual)" }
            }

            if (destRoot.exists()) destRoot.deleteRecursively()
            destRoot.mkdirs()

            val am = context.assets
            val assetFiles = collectAssetFiles(am, assetSubdir)
            var totalBytes = 0L
            assetFiles.forEach { rel ->
                // rel is "voices/en_US-amy-low/foo/bar" — strip the subdir
                // prefix so we land at destRoot/foo/bar.
                val relative = rel.removePrefix("$assetSubdir/")
                val outFile = File(destRoot, relative)
                outFile.parentFile?.mkdirs()
                am.open(rel).use { input ->
                    FileOutputStream(outFile).use { output ->
                        totalBytes += input.copyTo(output)
                    }
                }
            }
            manifestFile.writeText("${assetFiles.size}\n$totalBytes\n")
            log.i { "Mirrored ${assetFiles.size} voice assets (${totalBytes}B) to $destRoot" }
        }

        /**
         * Walk the asset directory at [path] and return every leaf file as an
         * asset-relative path. AssetManager.list() returns an empty array for
         * leaf files, so we attempt an open() to confirm.
         */
        private fun collectAssetFiles(am: AssetManager, path: String): List<String> {
            val entries = am.list(path).orEmpty()
            if (entries.isEmpty()) {
                return try {
                    am.open(path).close()
                    listOf(path)
                } catch (_: Throwable) {
                    emptyList()
                }
            }
            return entries.flatMap { child -> collectAssetFiles(am, "$path/$child") }
        }

        /**
         * Summarise the on-disk mirror as `<fileCount>\n<totalBytes>\n`,
         * excluding the manifest file itself. Cheap directory walk — no
         * content reads.
         */
        private fun manifestOnDisk(destRoot: File, manifestFile: File): String {
            var count = 0
            var bytes = 0L
            destRoot.walkTopDown().filter { it.isFile && it != manifestFile }
                .forEach { count++; bytes += it.length() }
            return "$count\n$bytes"
        }
    }
}
