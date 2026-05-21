package dev.ahmedmohamed.hayaitts.ui.speaker

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The SpeakerPicker intent extras are part of the public contract between
 * the picker and its launchers (Library overflow, VoiceDetail). If a rename
 * goes unreviewed, every existing pending intent and shortcut would break
 * silently. Pin the keys.
 */
class SpeakerPickerExtrasTest {

    @Test
    fun `extra voice id key is stable`() {
        assertEquals("voice_id", SpeakerPickerActivity.EXTRA_VOICE_ID)
    }

    @Test
    fun `extra speaker id key is stable`() {
        assertEquals("speaker_id", SpeakerPickerActivity.EXTRA_SPEAKER_ID)
    }
}
