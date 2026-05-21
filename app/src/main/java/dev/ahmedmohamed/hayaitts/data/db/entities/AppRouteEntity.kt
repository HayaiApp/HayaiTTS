package dev.ahmedmohamed.hayaitts.data.db.entities

import androidx.room.Entity

/**
 * Per-caller-package voice routing. When set, `HayaiTtsService` resolves the
 * calling UID to a package name and looks up the (package, locale) row to
 * override the global per-locale default voice.
 *
 * Primary key is composite — same caller can route different voices per
 * locale (e.g. Hayai → kokoro for en, piper-amy for ja).
 */
@Entity(
    tableName = "app_routes",
    primaryKeys = ["callerPackage", "locale"],
)
data class AppRouteEntity(
    val callerPackage: String,
    val locale: String,
    val voiceId: String,
    val sid: Int = 0,
)
