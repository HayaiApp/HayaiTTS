package dev.ahmedmohamed.hayaitts.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.ahmedmohamed.hayaitts.data.update.UpdateChannel
import dev.ahmedmohamed.hayaitts.domain.model.StorageLocation
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "hayai_settings")

class SettingsRepositoryImpl(
    private val context: Context,
) : SettingsRepository {

    private val store: androidx.datastore.core.DataStore<Preferences>
        get() = context.settingsDataStore

    override val wifiOnly: Flow<Boolean> = store.data.map { it[KEY_WIFI_ONLY] ?: DEFAULT_WIFI_ONLY }

    override suspend fun setWifiOnly(value: Boolean) {
        store.edit { it[KEY_WIFI_ONLY] = value }
    }

    override val storageLocation: Flow<StorageLocation> = store.data.map { prefs ->
        val raw = prefs[KEY_STORAGE_LOCATION] ?: return@map StorageLocation.INTERNAL
        runCatching { StorageLocation.valueOf(raw) }.getOrDefault(StorageLocation.INTERNAL)
    }

    override suspend fun setStorageLocation(value: StorageLocation) {
        store.edit { it[KEY_STORAGE_LOCATION] = value.name }
    }

    override val updateChannel: Flow<UpdateChannel> = store.data.map { prefs ->
        val raw = prefs[KEY_UPDATE_CHANNEL] ?: return@map UpdateChannel.STABLE
        runCatching { UpdateChannel.valueOf(raw) }.getOrDefault(UpdateChannel.STABLE)
    }

    override suspend fun setUpdateChannel(value: UpdateChannel) {
        store.edit { it[KEY_UPDATE_CHANNEL] = value.name }
    }

    override val lastUpdateCheckMillis: Flow<Long> = store.data.map { it[KEY_LAST_UPDATE_CHECK] ?: 0L }

    override suspend fun setLastUpdateCheckMillis(value: Long) {
        store.edit { it[KEY_LAST_UPDATE_CHECK] = value }
    }

    private companion object {
        // wifi-only defaults to true so first-time users on metered cell never
        // burn 85 MB without an explicit opt-in.
        const val DEFAULT_WIFI_ONLY = true
        val KEY_WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val KEY_STORAGE_LOCATION = stringPreferencesKey("storage_location")
        val KEY_UPDATE_CHANNEL = stringPreferencesKey("update_channel")
        val KEY_LAST_UPDATE_CHECK = longPreferencesKey("last_update_check_ms")
    }
}
