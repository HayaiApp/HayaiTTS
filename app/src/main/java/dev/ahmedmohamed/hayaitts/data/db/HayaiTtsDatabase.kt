package dev.ahmedmohamed.hayaitts.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.ahmedmohamed.hayaitts.data.db.dao.DefaultVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.dao.DownloadStateDao
import dev.ahmedmohamed.hayaitts.data.db.dao.InstalledVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.entities.DefaultVoiceEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.InstalledVoiceEntity

@Database(
    entities = [
        InstalledVoiceEntity::class,
        DownloadStateEntity::class,
        DefaultVoiceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class HayaiTtsDatabase : RoomDatabase() {
    abstract fun installedVoiceDao(): InstalledVoiceDao
    abstract fun downloadStateDao(): DownloadStateDao
    abstract fun defaultVoiceDao(): DefaultVoiceDao

    companion object {
        private const val DB_NAME = "hayai_tts.db"

        fun build(context: Context): HayaiTtsDatabase = Room
            .databaseBuilder(context.applicationContext, HayaiTtsDatabase::class.java, DB_NAME)
            // Phase 4a only has one schema version; we leave migrations off so
            // any accidental schema drift during development is loud rather
            // than silently corrupting installed-voice metadata.
            .build()
    }
}
