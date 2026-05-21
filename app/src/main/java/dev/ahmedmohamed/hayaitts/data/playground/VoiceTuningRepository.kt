package dev.ahmedmohamed.hayaitts.data.playground

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playgroundTuningStore by preferencesDataStore(name = "playground_tuning")

class VoiceTuningRepository(private val context: Context) {
    private val store get() = context.playgroundTuningStore

    fun tuningFor(voiceId: String): Flow<VoiceTuning> = store.data.map { prefs -> prefs.toTuning(voiceId) }

    suspend fun setTuning(voiceId: String, tuning: VoiceTuning) {
        store.edit { prefs ->
            prefs[speedKey(voiceId)] = tuning.speed.coerceIn(VoiceTuning.SPEED_MIN, VoiceTuning.SPEED_MAX)
            prefs[pitchKey(voiceId)] = tuning.pitch.coerceIn(VoiceTuning.PITCH_MIN, VoiceTuning.PITCH_MAX)
            prefs[lengthKey(voiceId)] = tuning.lengthScale.coerceIn(VoiceTuning.LENGTH_MIN, VoiceTuning.LENGTH_MAX)
            prefs[noiseScaleKey(voiceId)] = tuning.noiseScale.coerceIn(VoiceTuning.NOISE_SCALE_MIN, VoiceTuning.NOISE_SCALE_MAX)
            prefs[noiseScaleWKey(voiceId)] = tuning.noiseScaleW.coerceIn(VoiceTuning.NOISE_SCALE_W_MIN, VoiceTuning.NOISE_SCALE_W_MAX)
        }
    }

    private fun Preferences.toTuning(voiceId: String) = VoiceTuning(
        speed = this[speedKey(voiceId)] ?: 1f,
        pitch = this[pitchKey(voiceId)] ?: 1f,
        lengthScale = this[lengthKey(voiceId)] ?: 1f,
        noiseScale = this[noiseScaleKey(voiceId)] ?: 0.667f,
        noiseScaleW = this[noiseScaleWKey(voiceId)] ?: 0.8f,
    )

    private fun speedKey(voiceId: String) = floatPreferencesKey("speed_${voiceId}")
    private fun pitchKey(voiceId: String) = floatPreferencesKey("pitch_${voiceId}")
    private fun lengthKey(voiceId: String) = floatPreferencesKey("length_${voiceId}")
    private fun noiseScaleKey(voiceId: String) = floatPreferencesKey("noise_scale_${voiceId}")
    private fun noiseScaleWKey(voiceId: String) = floatPreferencesKey("noise_scale_w_${voiceId}")
}
