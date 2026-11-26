package dev.ahmedmohamed.hayaitts.tts

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsLocalesTest {

    @Test
    fun `framework iso3 codes become bcp47`() {
        assertEquals("en-US", TtsLocales.toBcp47("eng", "USA"))
        assertEquals("de-DE", TtsLocales.toBcp47("deu", "DEU"))
        assertEquals("de", TtsLocales.toBcp47("deu", ""))
        assertEquals("en-GB", TtsLocales.toBcp47("en", "GB"))
        assertEquals("en-US", TtsLocales.toBcp47("en_US"))
        assertEquals("", TtsLocales.toBcp47(null))
    }

    @Test
    fun `match level distinguishes country from language`() {
        assertEquals(TtsLocales.COUNTRY, TtsLocales.matchLevel(listOf("en-US"), "en-US"))
        assertEquals(TtsLocales.LANGUAGE, TtsLocales.matchLevel(listOf("en-GB"), "en-US"))
        assertEquals(TtsLocales.LANGUAGE, TtsLocales.matchLevel(listOf("en"), "en-US"))
        assertEquals(TtsLocales.NOT_SUPPORTED, TtsLocales.matchLevel(listOf("de-DE"), "en-US"))
        assertEquals(TtsLocales.COUNTRY, TtsLocales.matchLevel(listOf("en_US"), TtsLocales.toBcp47("eng", "USA")))
    }

    @Test
    fun `iso3 form for check-voice-data`() {
        assertEquals("eng-USA", TtsLocales.toIso3("en-US"))
        assertEquals("deu", TtsLocales.toIso3("de"))
    }
}
