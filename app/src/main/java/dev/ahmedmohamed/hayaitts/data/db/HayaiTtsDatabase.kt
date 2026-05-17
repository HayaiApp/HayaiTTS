package dev.ahmedmohamed.hayaitts.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.ahmedmohamed.hayaitts.data.db.dao.DefaultVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.dao.DownloadStateDao
import dev.ahmedmohamed.hayaitts.data.db.dao.InstalledVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.dao.PlaygroundSampleDao
import dev.ahmedmohamed.hayaitts.data.db.entities.DefaultVoiceEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.InstalledVoiceEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.PlaygroundSampleEntity

@Database(
    entities = [
        InstalledVoiceEntity::class,
        DownloadStateEntity::class,
        DefaultVoiceEntity::class,
        PlaygroundSampleEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class HayaiTtsDatabase : RoomDatabase() {
    abstract fun installedVoiceDao(): InstalledVoiceDao
    abstract fun downloadStateDao(): DownloadStateDao
    abstract fun defaultVoiceDao(): DefaultVoiceDao
    abstract fun playgroundSampleDao(): PlaygroundSampleDao

    companion object {
        private const val DB_NAME = "hayai_tts.db"

        /**
         * v1 -> v2 (Phase 6): add `effectiveFamily TEXT` to `installed_voices`.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE installed_voices ADD COLUMN effectiveFamily TEXT DEFAULT NULL")
            }
        }

        /**
         * v2 -> v3 (P5 Playground): add the `playground_samples` history table.
         * Empty on first run after the bump so no data-loss risk for existing voices.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `playground_samples` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`voiceId` TEXT NOT NULL, " +
                        "`text` TEXT NOT NULL, " +
                        "`sid` INTEGER NOT NULL, " +
                        "`speed` REAL NOT NULL, " +
                        "`pitch` REAL NOT NULL, " +
                        "`lengthScale` REAL NOT NULL, " +
                        "`pcmPath` TEXT NOT NULL, " +
                        "`sampleRate` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL" +
                        ")",
                )
            }
        }

        fun build(context: Context): HayaiTtsDatabase = Room
            .databaseBuilder(context.applicationContext, HayaiTtsDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }
}
