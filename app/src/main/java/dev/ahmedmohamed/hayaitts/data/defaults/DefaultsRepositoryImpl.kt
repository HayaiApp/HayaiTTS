package dev.ahmedmohamed.hayaitts.data.defaults

import dev.ahmedmohamed.hayaitts.data.db.dao.DefaultVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.entities.DefaultVoiceEntity
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultsRepositoryImpl(
    private val dao: DefaultVoiceDao,
) : DefaultsRepository {

    override val defaults: Flow<Map<String, String>> = dao.getAll().map { rows ->
        rows.associate { it.locale to it.voiceId }
    }

    override suspend fun setDefault(locale: String, voiceId: String) {
        dao.upsert(DefaultVoiceEntity(locale = locale, voiceId = voiceId))
    }

    override suspend fun clearDefault(locale: String) {
        dao.deleteByLocale(locale)
    }
}
