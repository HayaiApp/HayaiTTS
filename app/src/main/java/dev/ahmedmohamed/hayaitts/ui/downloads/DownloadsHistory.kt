package dev.ahmedmohamed.hayaitts.ui.downloads

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.downloadsHistoryStore by preferencesDataStore(name = "downloads_history")

/**
 * Persistent history of finished downloads. Shadows the Room download_states
 * table so the Downloads Manager's "Recently completed" group survives a
 * clearCompleted() purge or an app restart.
 */
class DownloadsHistory(private val context: Context) {

    private val store: androidx.datastore.core.DataStore<Preferences>
        get() = context.downloadsHistoryStore

    val entries: Flow<List<Entry>> = store.data.map { prefs -> parse(prefs[KEY_ENTRIES]) }

    suspend fun recordCompletion(voiceId: String, completedAtMillis: Long = System.currentTimeMillis()) {
        store.edit { prefs ->
            val current = parse(prefs[KEY_ENTRIES]).toMutableList()
            current.removeAll { it.voiceId == voiceId }
            current.add(0, Entry(voiceId, completedAtMillis))
            val trimmed = current.take(MAX_ENTRIES)
            prefs[KEY_ENTRIES] = serialise(trimmed)
        }
    }

    suspend fun remove(voiceId: String) {
        store.edit { prefs ->
            val filtered = parse(prefs[KEY_ENTRIES]).filterNot { it.voiceId == voiceId }
            if (filtered.isEmpty()) prefs.remove(KEY_ENTRIES) else prefs[KEY_ENTRIES] = serialise(filtered)
        }
    }

    suspend fun clearAll() {
        store.edit { prefs -> prefs.remove(KEY_ENTRIES) }
    }

    data class Entry(val voiceId: String, val completedAtMillis: Long)

    private fun parse(raw: String?): List<Entry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(SEPARATOR).mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size != 2) return@mapNotNull null
            val ts = parts[1].toLongOrNull() ?: return@mapNotNull null
            Entry(parts[0], ts)
        }
    }

    private fun serialise(entries: List<Entry>): String =
        entries.joinToString(SEPARATOR) { "${it.voiceId}:${it.completedAtMillis}" }

    companion object {
        const val MAX_ENTRIES = 50
        private const val SEPARATOR = "|"
        private val KEY_ENTRIES = stringPreferencesKey("entries")
    }
}
