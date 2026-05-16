package dev.ahmedmohamed.hayaitts.domain.repo

import dev.ahmedmohamed.hayaitts.domain.model.StorageLocation
import kotlinx.coroutines.flow.Flow

/**
 * DataStore-backed user preferences that the downloader and runtime react to.
 */
interface SettingsRepository {
    val wifiOnly: Flow<Boolean>
    suspend fun setWifiOnly(value: Boolean)

    val storageLocation: Flow<StorageLocation>
    suspend fun setStorageLocation(value: StorageLocation)
}
