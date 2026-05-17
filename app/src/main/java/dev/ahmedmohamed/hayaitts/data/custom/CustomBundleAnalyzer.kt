package dev.ahmedmohamed.hayaitts.data.custom

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Cheap, read-only inspection of a user-picked TTS bundle. Enumerates entries
 * inside `.tar.bz2` / `.tar` / `.zip` archives or treats a bare `.onnx` file as
 * a single-file Piper-style bundle. Never extracts to disk — that happens in
 * [CustomBundleInstaller] once the user has confirmed the wizard.
 *
 * The analyzer is best-effort: speakers detection falls back to a single
 * `default` speaker (sid 0) when the bundle does not surface a speaker list,
 * which is the case for every Piper voice and most VITS checkpoints.
 */
class CustomBundleAnalyzer(private val context: Context) {

    private val log = Logger.withTag("CustomBundleAnalyzer")

    data class Analysis(
        /** Original filename (best-effort from SAF; may be null). */
        val originalName: String?,
        /** Bundle archive kind we detected from name + magic bytes. */
        val archive: ArchiveKind,
        /** Family we guessed from the contents; never CUSTOM. */
        val detectedFamily: ModelFamily?,
        /** Why detection landed where it did (surfaced in the UI as a help line). */
        val familyReason: String,
        /** Whether we recognised this as a Kokoro/Kitten bundle we refuse. */
        val unsupportedFamily: ModelFamily?,
        /** Flat file listing — name + uncompressed size in bytes. */
        val entries: List<Entry>,
        /** Best-effort BCP-47 tags (may be empty if we cannot guess). */
        val detectedLanguages: List<String>,
        /** Best-effort speaker list. Always non-empty. */
        val speakers: List<Speaker>,
        /** Default voice name we suggest in the wizard. */
        val suggestedName: String,
    )

    enum class ArchiveKind { TAR_BZ2, TAR, ZIP, BARE_ONNX, UNKNOWN }

    data class Entry(val path: String, val sizeBytes: Long)

    fun analyze(uri: Uri): Result<Analysis> = runCatching {
        val originalName = queryDisplayName(uri)
        val archive = detectArchive(originalName)
        // `inlineFiles` captures the bytes of every small (<64 KB) text /
        // JSON / list file inside the archive as we stream past it, so we
        // can read speaker manifests without a second pass over the input
        // stream (which SAF URIs do not always support — many resolvers are
        // one-shot).
        val inlineFiles = mutableMapOf<String, ByteArray>()
        val entries = when (archive) {
            ArchiveKind.TAR_BZ2 -> openTarBz2(uri).use { listTar(it, inlineFiles) }
            ArchiveKind.TAR -> openStream(uri).use { listTar(TarArchiveInputStream(it), inlineFiles) }
            ArchiveKind.ZIP -> openStream(uri).use { listZip(it, inlineFiles) }
            ArchiveKind.BARE_ONNX -> listOf(
                Entry(path = originalName ?: "model.onnx", sizeBytes = sizeOf(uri)),
            )
            ArchiveKind.UNKNOWN -> error("Unsupported file type")
        }
        if (entries.isEmpty()) error("Bundle is empty")

        val (family, reason, unsupported) = classifyFamily(entries, archive)
        val langs = guessLanguages(originalName, entries)
        val speakers = guessSpeakersFrom(inlineFiles)
        val suggested = sanitizeName(originalName ?: "custom-voice")
        Analysis(
            originalName = originalName,
            archive = archive,
            detectedFamily = family,
            familyReason = reason,
            unsupportedFamily = unsupported,
            entries = entries.sortedBy { it.path },
            detectedLanguages = langs,
            speakers = speakers,
            suggestedName = suggested,
        )
    }.onFailure { log.w(it) { "Analyze failed for $uri" } }

    // -- archive listing ----------------------------------------------------

    private fun openStream(uri: Uri): InputStream =
        BufferedInputStream(
            context.contentResolver.openInputStream(uri) ?: error("Cannot open $uri"),
        )

    private fun openTarBz2(uri: Uri): TarArchiveInputStream {
        val raw = openStream(uri)
        val bz2 = BZip2CompressorInputStream(raw)
        return TarArchiveInputStream(bz2)
    }

    private fun listTar(
        tar: TarArchiveInputStream,
        inlineFiles: MutableMap<String, ByteArray>,
    ): List<Entry> {
        val out = mutableListOf<Entry>()
        var entry = tar.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                val path = normalize(entry.name)
                out += Entry(path = path, sizeBytes = entry.size)
                if (shouldCaptureInline(path, entry.size)) {
                    inlineFiles[path] = readBoundedBytes(tar, INLINE_MAX_BYTES)
                }
            }
            entry = tar.nextEntry
        }
        return out
    }

    private fun listZip(
        stream: InputStream,
        inlineFiles: MutableMap<String, ByteArray>,
    ): List<Entry> {
        val out = mutableListOf<Entry>()
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val path = normalize(entry.name)
                    val size = entry.size.coerceAtLeast(0L)
                    out += Entry(path = path, sizeBytes = size)
                    // ZIP `size` is sometimes -1 streaming, so probe by name + a
                    // bounded read; the bounded read also protects from giant
                    // entries whose declared size is bogus.
                    if (shouldCaptureInline(path, size)) {
                        inlineFiles[path] = readBoundedBytes(zip, INLINE_MAX_BYTES)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return out
    }

    /** Cap-limited read so a malicious bundle cannot OOM us via a giant "speakers.txt". */
    private fun readBoundedBytes(stream: InputStream, max: Int): ByteArray {
        val buf = ByteArrayOutputStream(minOf(max, 16 * 1024))
        val chunk = ByteArray(8 * 1024)
        var totalRead = 0
        while (totalRead < max) {
            val n = stream.read(chunk, 0, minOf(chunk.size, max - totalRead))
            if (n <= 0) break
            buf.write(chunk, 0, n)
            totalRead += n
        }
        return buf.toByteArray()
    }

    private fun shouldCaptureInline(path: String, sizeBytes: Long): Boolean {
        if (sizeBytes in 1..INLINE_MAX_BYTES.toLong()) {
            // Whitelist by name first to avoid pulling random small assets.
            val name = path.substringAfterLast('/').lowercase()
            return name in INLINE_NAME_WHITELIST ||
                name.endsWith(".json") ||
                name.endsWith(".yaml") ||
                name.endsWith(".yml") ||
                name == "speakers" || name == "voices"
        }
        return false
    }

    private fun normalize(raw: String): String =
        raw.replace('\\', '/').removePrefix("./").trimStart('/')

    // -- classification -----------------------------------------------------

    private data class FamilyDetection(
        val family: ModelFamily?,
        val reason: String,
        val unsupported: ModelFamily?,
    )

    private fun classifyFamily(entries: List<Entry>, archive: ArchiveKind): FamilyDetection {
        val names = entries.map { it.path.substringAfterLast('/') }
        val basenames = names.toSet()
        val onnxFiles = entries.filter { it.path.endsWith(".onnx", ignoreCase = true) }
        val hasTokens = basenames.contains(TOKENS_FILE)
        val hasLexicon = basenames.contains(LEXICON_FILE)
        val hasEspeak = entries.any { it.path.contains("espeak-ng-data/", ignoreCase = true) }
        val hasDict = entries.any { it.path.contains("dict/", ignoreCase = true) }
        val hasVoicesBin = basenames.contains("voices.bin")
        val hasModelOnnx = basenames.contains("model.onnx")

        if (hasVoicesBin) {
            return FamilyDetection(
                family = null,
                reason = "Bundle looks like a Kokoro voice (voices.bin present).",
                unsupported = ModelFamily.KOKORO,
            )
        }

        val matchaAcoustic = basenames.any {
            it == "acoustic.onnx" || it.startsWith("model-steps-")
        }
        val matchaVocoder = onnxFiles.any {
            val n = it.path.substringAfterLast('/').lowercase()
            n.contains("vocos") || n.contains("hifigan") || n.contains("vocoder")
        }
        if (matchaAcoustic && (matchaVocoder || onnxFiles.size >= 2)) {
            return FamilyDetection(ModelFamily.MATCHA, "Matcha: acoustic + vocoder onnx present.", null)
        }

        if (hasModelOnnx && hasTokens && hasLexicon && hasDict) {
            return FamilyDetection(ModelFamily.VITS, "VITS: model.onnx + lexicon.txt + dict/.", null)
        }
        if (hasModelOnnx && hasTokens && (hasEspeak || !hasLexicon)) {
            return FamilyDetection(ModelFamily.PIPER, "Piper: model.onnx + tokens.txt.", null)
        }
        if (hasModelOnnx && hasTokens) {
            return FamilyDetection(ModelFamily.VITS, "VITS: model.onnx + tokens.txt.", null)
        }
        if (archive == ArchiveKind.BARE_ONNX) {
            return FamilyDetection(
                family = ModelFamily.PIPER,
                reason = "Bare .onnx assumed to be Piper — you must provide a matching tokens.txt later.",
                unsupported = null,
            )
        }
        return FamilyDetection(
            family = null,
            reason = "Could not infer family from contents — choose one below.",
            unsupported = null,
        )
    }

    private fun guessLanguages(originalName: String?, entries: List<Entry>): List<String> {
        // Most Piper bundles encode the locale in the filename, e.g.
        // `en_US-amy-low.tar.bz2`. Pull the first `xx_YY` token if present.
        val hay = (originalName.orEmpty() + " " + entries.joinToString(" ") { it.path }).lowercase()
        val match = Regex("""([a-z]{2})_([a-z]{2,3})""").find(hay) ?: return emptyList()
        val bcp = "${match.groupValues[1]}-${match.groupValues[2].uppercase()}"
        return listOf(bcp)
    }

    /**
     * Build a speaker list by reading inline manifest files captured during
     * the streaming archive pass. Tries (in order) `speakers.txt`, `voices.txt`,
     * any `*.json` with a `speaker_id_map` key or a top-level `speakers` array,
     * then falls back to the single `default` speaker.
     *
     * `speakers.txt` is the de-facto upstream sherpa-onnx format for VCTK /
     * AISHELL3 / similar multi-speaker bundles — one name per line, sid 0 ..
     * N-1 implicit from the line order.
     */
    private fun guessSpeakersFrom(inlineFiles: Map<String, ByteArray>): List<Speaker> {
        fun bytesOf(suffix: String): ByteArray? = inlineFiles.entries
            .firstOrNull { it.key.substringAfterLast('/').equals(suffix, ignoreCase = true) }
            ?.value

        bytesOf("speakers.txt")?.let { parseLineList(it) }?.takeIf { it.isNotEmpty() }
            ?.let { return it.toSpeakers() }
        bytesOf("voices.txt")?.let { parseLineList(it) }?.takeIf { it.isNotEmpty() }
            ?.let { return it.toSpeakers() }

        // Try JSON files: anything with `speaker_id_map` (sherpa's piper export)
        // or a top-level `speakers` / `voices` array.
        for ((path, bytes) in inlineFiles) {
            if (!path.endsWith(".json", ignoreCase = true)) continue
            val parsed = runCatching {
                jsonFormat.parseToJsonElement(bytes.decodeToString())
            }.getOrNull() ?: continue
            speakersFromJson(parsed)?.takeIf { it.isNotEmpty() }?.let { return it }
        }

        return listOf(Speaker(id = 0, name = "default", gender = "unknown"))
    }

    private fun parseLineList(bytes: ByteArray): List<String> = bytes
        .decodeToString()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toList()

    private fun List<String>.toSpeakers(): List<Speaker> = mapIndexed { idx, name ->
        Speaker(id = idx, name = name, gender = "unknown")
    }

    private fun speakersFromJson(root: JsonElement): List<Speaker>? {
        if (root !is JsonObject) return null
        // sherpa-onnx Piper / VITS json layouts:
        //   { "speaker_id_map": { "amy": 0, "bob": 1 } }
        //   { "speakers": ["amy", "bob"] }
        //   { "voices": [{"name": "amy", "id": 0, "gender": "F"}] }
        (root["speaker_id_map"] as? JsonObject)?.let { obj ->
            return obj.entries
                .mapNotNull { (name, idEl) ->
                    val id = (idEl as? JsonPrimitive)?.intOrNull ?: return@mapNotNull null
                    Speaker(id = id, name = name, gender = "unknown")
                }
                .sortedBy { it.id }
        }
        (root["speakers"] as? JsonArray)?.let { arr ->
            return arr.mapIndexedNotNull { idx, el ->
                when (el) {
                    is JsonPrimitive -> Speaker(idx, el.contentOrNull ?: "speaker_$idx", "unknown")
                    is JsonObject -> {
                        val name = (el["name"] as? JsonPrimitive)?.contentOrNull ?: "speaker_$idx"
                        val sid = (el["id"] as? JsonPrimitive)?.intOrNull ?: idx
                        val gender = (el["gender"] as? JsonPrimitive)?.contentOrNull ?: "unknown"
                        Speaker(id = sid, name = name, gender = gender)
                    }
                    else -> null
                }
            }
        }
        (root["voices"] as? JsonArray)?.let { arr ->
            return arr.mapIndexedNotNull { idx, el ->
                (el as? JsonObject)?.let { obj ->
                    val name = (obj["name"] as? JsonPrimitive)?.contentOrNull ?: "speaker_$idx"
                    val sid = (obj["id"] as? JsonPrimitive)?.intOrNull ?: idx
                    val gender = (obj["gender"] as? JsonPrimitive)?.contentOrNull ?: "unknown"
                    Speaker(id = sid, name = name, gender = gender)
                }
            }
        }
        return null
    }

    private fun sanitizeName(raw: String): String {
        val stripped = raw
            .substringBeforeLast('.')
            .substringBeforeLast('.') // strip both `.tar.bz2` halves
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('_')
        return stripped.ifBlank { "custom-voice" }
    }

    private fun detectArchive(name: String?): ArchiveKind {
        val n = name?.lowercase().orEmpty()
        return when {
            n.endsWith(".tar.bz2") || n.endsWith(".tbz2") || n.endsWith(".tbz") -> ArchiveKind.TAR_BZ2
            n.endsWith(".zip") -> ArchiveKind.ZIP
            n.endsWith(".tar") -> ArchiveKind.TAR
            n.endsWith(".onnx") -> ArchiveKind.BARE_ONNX
            else -> ArchiveKind.UNKNOWN
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && idx >= 0) cursor.getString(idx) else null
            }
        }.getOrNull()
    }

    private fun sizeOf(uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && idx >= 0) cursor.getLong(idx) else 0L
        } ?: 0L
    }.getOrDefault(0L)

    private companion object {
        const val TOKENS_FILE = "tokens.txt"
        const val LEXICON_FILE = "lexicon.txt"

        /** Cap on the in-memory capture of inline metadata files. 64 KB covers
         *  every realistic speakers.txt (VCTK = 109 names × ~6 bytes = ~700 B)
         *  while protecting against a malicious 64 MB "speakers.txt". */
        const val INLINE_MAX_BYTES = 64 * 1024

        /** Files we always try to capture if they appear in the archive. */
        val INLINE_NAME_WHITELIST = setOf(
            "speakers.txt", "voices.txt", "speaker_list.txt", "speaker_ids.txt",
            "config.json", "model_config.json", "metadata.json", "voices.json",
            "speakers.json", "model_card.json",
        )

        val jsonFormat = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
