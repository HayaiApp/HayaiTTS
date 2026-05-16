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
import dev.ahmedmohamed.hayaitts.data.db.entities.DefaultVoiceEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.DownloadStateEntity
import dev.ahmedmohamed.hayaitts.data.db.entities.InstalledVoiceEntity

@Database(
    entities = [
        InstalledVoiceEntity::class,
        DownloadStateEntity::class,
        DefaultVoiceEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class HayaiTtsDatabase : RoomDatabase() {
    abstract fun installedVoiceDao(): InstalledVoiceDao
    abstract fun downloadStateDao(): DownloadStateDao
    abstract fun defaultVoiceDao(): DefaultVoiceDao

    companion object {
        private const val DB_NAME = "hayai_tts.db"

        /**
         * v1 -> v2 (Phase 6): add `effectiveFamily TEXT` to `installed_voices`
         * so a row with `family='custom'` can record which real runtime family
         * (piper / vits / matcha) the user-imported bundle is wired to.
         * Existing rows get NULL, which is correct for non-custom voices.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE installed_voices ADD COLUMN effectiveFamily TEXT DEFAULT NULL",
                )
            }
        }

        fun build(context: Context): HayaiTtsDatabase = Room
            .databaseBuilder(context.applicationContext, HayaiTtsDatabase::class.java, DB_NAME)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
}
