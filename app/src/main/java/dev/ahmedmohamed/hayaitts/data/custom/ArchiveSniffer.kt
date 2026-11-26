package dev.ahmedmohamed.hayaitts.data.custom

import java.io.InputStream

/** Identifies a bundle by its leading bytes when the filename is unavailable. */
internal object ArchiveSniffer {

    private const val HEADER_BYTES = 512

    fun sniff(input: InputStream): CustomBundleAnalyzer.ArchiveKind {
        val head = ByteArray(HEADER_BYTES)
        var n = 0
        while (n < HEADER_BYTES) {
            val r = input.read(head, n, HEADER_BYTES - n)
            if (r <= 0) break
            n += r
        }
        return sniff(head.copyOf(n))
    }

    fun sniff(head: ByteArray): CustomBundleAnalyzer.ArchiveKind = when {
        head.startsWith("BZh") -> CustomBundleAnalyzer.ArchiveKind.TAR_BZ2
        head.startsWith(byteArrayOf(0x50, 0x4B, 0x03, 0x04)) -> CustomBundleAnalyzer.ArchiveKind.ZIP
        head.size >= 262 && String(head, 257, 5, Charsets.US_ASCII) == "ustar" -> CustomBundleAnalyzer.ArchiveKind.TAR
        // ONNX is a protobuf ModelProto; field 1 (ir_version, varint) encodes as 0x08.
        head.isNotEmpty() && head[0] == 0x08.toByte() -> CustomBundleAnalyzer.ArchiveKind.BARE_ONNX
        else -> CustomBundleAnalyzer.ArchiveKind.UNKNOWN
    }

    private fun ByteArray.startsWith(prefix: String) = startsWith(prefix.toByteArray(Charsets.US_ASCII))

    private fun ByteArray.startsWith(prefix: ByteArray): Boolean =
        size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
}
