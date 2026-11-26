package dev.ahmedmohamed.hayaitts.tts

import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class KokoroLanguageTest {

    @get:Rule val tmp = TemporaryFolder()

    private fun espeak(vararg voices: String): File {
        val data = tmp.newFolder("espeak-ng-data")
        voices.forEach { path -> File(data, "lang/$path").apply { parentFile!!.mkdirs(); writeText("x") } }
        return data
    }

    @Test
    fun `zh and en use the bundled lexicons, never an espeak voice`() {
        // There is no espeak voice file named "zh" (it's sit/cmn); passing it aborts sherpa-onnx.
        val data = espeak("sit/cmn", "gmw/en-US")
        assertEquals("", KokoroLanguage.espeakVoiceFor("zh", data))
        assertEquals("", KokoroLanguage.espeakVoiceFor("zh-CN", data))
        assertEquals("", KokoroLanguage.espeakVoiceFor("en-US", data))
    }

    @Test
    fun `other languages map to an existing espeak voice file`() {
        val data = espeak("roa/es", "roa/fr", "roa/pt-BR", "inc/hi")
        assertEquals("es", KokoroLanguage.espeakVoiceFor("es", data))
        assertEquals("fr", KokoroLanguage.espeakVoiceFor("fr-FR", data))
        assertEquals("pt-BR", KokoroLanguage.espeakVoiceFor("pt_BR", data))
        assertEquals("hi", KokoroLanguage.espeakVoiceFor("hi", data))
    }

    @Test
    fun `unknown voice file falls back to empty instead of crashing`() {
        assertEquals("", KokoroLanguage.espeakVoiceFor("it", espeak("roa/es")))
        assertEquals("", KokoroLanguage.espeakVoiceFor("es", null))
    }
}
