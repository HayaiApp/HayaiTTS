package dev.ahmedmohamed.hayaitts.ui.library.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.libraryUiDataStore by preferencesDataStore(name = "library_ui")

/**
 * UI-only preferences for the Library screen — voice ordering (for long-press
 * drag reorder) and the favorites set (starred voices pinned to the top of
 * their language group).
 *
 * Stored as a separate DataStore from `hayai_settings` so the Library reorder
 * UI never accidentally races against engine-side preferences (wifi-only,
 * storage location). Format is intentionally trivial:
 *
 *  - `voice_order`: comma-separated list of voiceIds in user-chosen order.
 *  - `favorite_voices`: Set<voiceId>.
 */
class LibraryUiPreferences(private val context: Context) {

    private val store: androidx.datastore.core.DataStore<Preferences>
        get() = context.libraryUiDataStore

    val voiceOrder: Flow<List<String>> = store.data.map { prefs ->
        prefs[KEY_VOICE_ORDER]
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    val favoriteVoices: Flow<Set<String>> = store.data.map { prefs ->
        prefs[KEY_FAVORITES] ?: emptySet()
    }

    suspend fun setVoiceOrder(order: List<String>) {
        store.edit { prefs ->
            if (order.isEmpty()) {
                prefs.remove(KEY_VOICE_ORDER)
            } else {
                prefs[KEY_VOICE_ORDER] = order.joinToString(SEPARATOR)
            }
        }
    }

    suspend fun toggleFavorite(voiceId: String) {
        store.edit { prefs ->
            val current = prefs[KEY_FAVORITES].orEmpty()
            prefs[KEY_FAVORITES] = if (voiceId in current) current - voiceId else current + voiceId
        }
    }

    private companion object {
        const val SEPARATOR = ","
        val KEY_VOICE_ORDER = stringPreferencesKey("voice_order")
        val KEY_FAVORITES = stringSetPreferencesKey("favorite_voices")
    }
}
