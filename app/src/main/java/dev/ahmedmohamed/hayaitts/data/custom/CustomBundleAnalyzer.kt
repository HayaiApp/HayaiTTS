package dev.ahmedmohamed.hayaitts.data.custom

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import co.touchlab.kermit.Logger
import dev.ahmedmohamed.hayaitts.domain.model.ModelFamily
import dev.ahmedmohamed.hayaitts.domain.model.Speaker
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
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
        val entries = when (archive) {
            ArchiveKind.TAR_BZ2 -> openTarBz2(uri).use { listTar(it) }
            ArchiveKind.TAR -> openStream(uri).use { listTar(TarArchiveInputStream(it)) }
            ArchiveKind.ZIP -> openStream(uri).use { listZip(it) }
            ArchiveKind.BARE_ONNX -> listOf(
                Entry(path = originalName ?: "model.onnx", sizeBytes = sizeOf(uri)),
            )
            ArchiveKind.UNKNOWN -> error("Unsupported file type")
        }
        if (entries.isEmpty()) error("Bundle is empty")

        val (family, reason, unsupported) = classifyFamily(entries, archive)
        val langs = guessLanguages(originalName, entries)
        val speakers = guessSpeakers()
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

    private fun listTar(tar: TarArchiveInputStream): List<Entry> {
        val out = mutableListOf<Entry>()
        var entry = tar.nextEntry
        while (entry != null) {
            if (!entry.isDirectory) {
                out += Entry(
                    path = normalize(entry.name),
                    sizeBytes = entry.size,
                )
            }
            entry = tar.nextEntry
        }
        return out
    }

    private fun listZip(stream: InputStream): List<Entry> {
        val out = mutableListOf<Entry>()
        ZipInputStream(stream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    out += Entry(
                        path = normalize(entry.name),
                        // ZIP entries from a streaming pass do not always have
                        // size populated; fall back to 0 when -1.
                        sizeBytes = entry.size.coerceAtLeast(0L),
                    )
                }
                entry = zip.nextEntry
            }
        }
        return out
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

    private fun guessSpeakers(): List<Speaker> =
        listOf(Speaker(id = 0, name = "default", gender = "unknown"))

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
    }
}
