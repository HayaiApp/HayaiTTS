package dev.ahmedmohamed.hayaitts.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.ahmedmohamed.hayaitts.data.db.dao.AppRouteDao
import dev.ahmedmohamed.hayaitts.data.db.dao.DefaultVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.dao.DownloadStateDao
import dev.ahmedmohamed.hayaitts.data.db.dao.InstalledVoiceDao
import dev.ahmedmohamed.hayaitts.data.db.dao.PlaygroundSampleDao
import dev.ahmedmohamed.hayaitts.data.db.dao.PronunciationDao
import dev.ahmedmohamed.hayaitts.data.db.dao.VoiceProfileDao
import dev.ahmedmohamed.hayaitts.data.db.entities.AppRouteEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.DefaultVoiceEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.InstalledVoiceEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.PlaygroundSampleEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.PronunciationEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.VoiceProfileEntity

@Database(
    entities = [
        InstalledVoiceEntity::class,
        DownloadStateEntity::class,
        DefaultVoiceEntity::class,
        PlaygroundSampleEntity::class,
        VoiceProfileEntity::class,
        PronunciationEntity::class,
        AppRouteEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class HayaiTtsDatabase : RoomDatabase() {
    abstract fun installedVoiceDao(): InstalledVoiceDao
    abstract fun downloadStateDao(): DownloadStateDao
    abstract fun defaultVoiceDao(): DefaultVoiceDao
    abstract fun playgroundSampleDao(): PlaygroundSampleDao
    abstract fun voiceProfileDao(): VoiceProfileDao
    abstract fun pronunciationDao(): PronunciationDao
    abstract fun appRouteDao(): AppRouteDao

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

        /**
         * v3 -> v4 (Phase 7 + Phase 4): three new tables — saved voice
         * profiles, pronunciation overrides, per-app voice routes. Empty on
         * first run so nothing migrates.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `voice_profiles` (" +
                        "`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                        "`voiceId` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`sid` INTEGER NOT NULL, " +
                        "`speed` REAL NOT NULL, " +
                        "`pitch` REAL NOT NULL, " +
                        "`lengthScale` REAL NOT NULL, " +
                        "`noiseScale` REAL NOT NULL, " +
                        "`noiseScaleW` REAL NOT NULL, " +
                        "`ssmlEnabled` INTEGER NOT NULL, " +
                        "`isDefaultForVoice` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL" +
                        ")",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `pronunciations` (" +
                        "`locale` TEXT NOT NULL, " +
                        "`word` TEXT NOT NULL, " +
                        "`replacement` TEXT NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`locale`, `word`)" +
                        ")",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `app_routes` (" +
                        "`callerPackage` TEXT NOT NULL, " +
                        "`locale` TEXT NOT NULL, " +
                        "`voiceId` TEXT NOT NULL, " +
                        "`sid` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`callerPackage`, `locale`)" +
                        ")",
                )
            }
        }

        fun build(context: Context): HayaiTtsDatabase = Room
            .databaseBuilder(context.applicationContext, HayaiTtsDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()
    }
}
