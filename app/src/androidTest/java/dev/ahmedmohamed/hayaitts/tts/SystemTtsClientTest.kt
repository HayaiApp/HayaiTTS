package dev.ahmedmohamed.hayaitts.tts

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Drives HayaiTTS exactly like a third-party app (browser read-aloud, a
 * reader) does: through the framework `TextToSpeech` client, from another
 * process. Needs at least one installed English voice; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
class SystemTtsClientTest {

    private val ctx = InstrumentationRegistry.getInstrumentation().context
    private lateinit var tts: TextToSpeech

    @Before
    fun bind() {
        val ready = CountDownLatch(1)
        var status = TextToSpeech.ERROR
        tts = TextToSpeech(ctx, { status = it; ready.countDown() }, ENGINE)
        assertTrue("engine bind timed out", ready.await(20, TimeUnit.SECONDS))
        assertEquals(TextToSpeech.SUCCESS, status)
    }

    @After
    fun release() = tts.shutdown()

    @Test
    fun setLanguageFindsAnInstalledVoiceAndSynthesizesAudio() {
        assumeTrue("no English voice installed", tts.isLanguageAvailable(Locale.US) >= TextToSpeech.LANG_AVAILABLE)

        val result = tts.setLanguage(Locale.US)
        assertTrue("setLanguage returned $result", result >= TextToSpeech.LANG_AVAILABLE)
        assertNotNull("no default voice after setLanguage", tts.voice)

        val out = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "hayai-system-tts.wav").apply { delete() }
        val done = CountDownLatch(1)
        var error: Int? = null
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) = done.countDown()
            @Deprecated("old API") override fun onError(id: String?) { error = -1; done.countDown() }
            override fun onError(id: String?, code: Int) { error = code; done.countDown() }
        })
        // Longer than one section, so streaming across sections is exercised.
        val text = "Hello from the system text to speech path. This second sentence makes sure " +
            "sections stream one after another. And a third, for good measure."
        assertEquals(TextToSpeech.SUCCESS, tts.synthesizeToFile(text, null, out, "u1"))
        assertTrue("synthesis timed out", done.await(60, TimeUnit.SECONDS))
        assertEquals(null, error)
        // 44-byte WAV header + at least ~1 s of 16 kHz 16-bit audio.
        assertTrue("wav too small: ${out.length()}", out.length() > 44 + 32_000)
    }

    private companion object {
        const val ENGINE = "dev.ahmedmohamed.hayaitts"
    }
}
