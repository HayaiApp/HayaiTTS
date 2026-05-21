package dev.ahmedmohamed.hayaitts.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadStateTest {

    @Test
    fun `idle queued done cancelled are singletons`() {
        assertTrue(DownloadState.Idle === DownloadState.Idle)
        assertTrue(DownloadState.Queued === DownloadState.Queued)
        assertTrue(DownloadState.Done === DownloadState.Done)
        assertTrue(DownloadState.Cancelled === DownloadState.Cancelled)
    }

    @Test
    fun `running preserves pct and byte counters`() {
        val s = DownloadState.Running(pct = 0.42f, downloadedBytes = 4_200L, totalBytes = 10_000L)
        assertEquals(0.42f, s.pct, 0f)
        assertEquals(4_200L, s.downloadedBytes)
        assertEquals(10_000L, s.totalBytes)
    }

    @Test
    fun `extracting carries a pct fraction`() {
        val s = DownloadState.Extracting(pct = 0.75f)
        assertEquals(0.75f, s.pct, 0f)
    }

    @Test
    fun `extracting equality compares the fraction`() {
        assertEquals(DownloadState.Extracting(0.5f), DownloadState.Extracting(0.5f))
        assertNotEquals(DownloadState.Extracting(0.5f), DownloadState.Extracting(0.6f))
    }

    @Test
    fun `failed carries the reason verbatim`() {
        val s = DownloadState.Failed("connection reset")
        assertEquals("connection reset", s.reason)
    }
}
