package dev.ahmedmohamed.hayaitts.data.custom

import dev.ahmedmohamed.hayaitts.data.custom.CustomBundleAnalyzer.ArchiveKind
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveSnifferTest {

    private fun tar(): ByteArray = ByteArrayOutputStream().also { bytes ->
        TarArchiveOutputStream(bytes).use { tar ->
            val data = "a b\n".toByteArray()
            tar.putArchiveEntry(TarArchiveEntry("voice/tokens.txt").apply { size = data.size.toLong() })
            tar.write(data)
            tar.closeArchiveEntry()
        }
    }.toByteArray()

    private fun sniff(bytes: ByteArray) = ArchiveSniffer.sniff(ByteArrayInputStream(bytes))

    @Test
    fun `tar bz2`() {
        val bz = ByteArrayOutputStream().also { out -> BZip2CompressorOutputStream(out).use { it.write(tar()) } }.toByteArray()
        assertEquals(ArchiveKind.TAR_BZ2, sniff(bz))
    }

    @Test
    fun `plain tar`() = assertEquals(ArchiveKind.TAR, sniff(tar()))

    @Test
    fun zip() {
        val zip = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { it.putNextEntry(ZipEntry("tokens.txt")); it.write(1); it.closeEntry() }
        }.toByteArray()
        assertEquals(ArchiveKind.ZIP, sniff(zip))
    }

    @Test
    fun `onnx protobuf header`() = assertEquals(ArchiveKind.BARE_ONNX, sniff(byteArrayOf(0x08, 0x07, 0x12, 0x07)))

    @Test
    fun `random text is unknown`() = assertEquals(ArchiveKind.UNKNOWN, sniff("hello world".toByteArray()))
}
