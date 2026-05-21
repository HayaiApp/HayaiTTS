package dev.ahmedmohamed.hayaitts.data.playground

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTuningTest {

    @Test
    fun `default is identity for speed pitch length`() {
        val d = VoiceTuning.Default
        assertEquals(1f, d.speed, 0f)
        assertEquals(1f, d.pitch, 0f)
        assertEquals(1f, d.lengthScale, 0f)
    }

    @Test
    fun `default noise values match sherpa-onnx VITS defaults`() {
        val d = VoiceTuning.Default
        assertEquals(0.667f, d.noiseScale, 0f)
        assertEquals(0.8f, d.noiseScaleW, 0f)
    }

    @Test
    fun `copy yields a distinct instance with changed field`() {
        val d = VoiceTuning.Default
        val tweaked = d.copy(speed = 1.2f)
        assertNotEquals(d, tweaked)
        assertEquals(1.2f, tweaked.speed, 0f)
        assertEquals(d.pitch, tweaked.pitch, 0f)
        assertEquals(d.lengthScale, tweaked.lengthScale, 0f)
        assertEquals(d.noiseScale, tweaked.noiseScale, 0f)
        assertEquals(d.noiseScaleW, tweaked.noiseScaleW, 0f)
    }

    @Test
    fun `ranges form non-empty intervals`() {
        assertTrue(VoiceTuning.SPEED_MAX > VoiceTuning.SPEED_MIN)
        assertTrue(VoiceTuning.PITCH_MAX > VoiceTuning.PITCH_MIN)
        assertTrue(VoiceTuning.LENGTH_MAX > VoiceTuning.LENGTH_MIN)
        assertTrue(VoiceTuning.NOISE_SCALE_MAX > VoiceTuning.NOISE_SCALE_MIN)
        assertTrue(VoiceTuning.NOISE_SCALE_W_MAX > VoiceTuning.NOISE_SCALE_W_MIN)
    }

    @Test
    fun `default speed pitch length sit within their declared ranges`() {
        val d = VoiceTuning.Default
        assertTrue(d.speed in VoiceTuning.SPEED_MIN..VoiceTuning.SPEED_MAX)
        assertTrue(d.pitch in VoiceTuning.PITCH_MIN..VoiceTuning.PITCH_MAX)
        assertTrue(d.lengthScale in VoiceTuning.LENGTH_MIN..VoiceTuning.LENGTH_MAX)
    }
}
