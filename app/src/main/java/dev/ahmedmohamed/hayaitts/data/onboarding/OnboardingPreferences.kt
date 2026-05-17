package dev.ahmedmohamed.hayaitts.data.onboarding

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One-shot flag persisted in a tiny DataStore so the first-launch onboarding
 * flow runs exactly once per fresh install. Kept in its own DataStore file
 * (separate from the main settings store) so a future user-initiated "reset
 * onboarding" action can wipe it without touching engine settings.
 *
 * Default: `false` (not completed) on first launch; flipped to `true` by the
 * onboarding flow's final "Get started" CTA.
 */
private val Context.onboardingDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hayai_onboarding",
)

class OnboardingPreferences(private val context: Context) {

    val isComplete: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETE] ?: false
    }

    suspend fun setComplete(value: Boolean) {
        context.onboardingDataStore.edit { it[KEY_ONBOARDING_COMPLETE] = value }
    }

    private companion object {
        val KEY_ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }
}
