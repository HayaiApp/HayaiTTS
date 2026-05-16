package dev.ahmedmohamed.hayaitts.data.catalog

import kotlinx.serialization.json.Json

/**
 * Shared [Json] used to (de)serialize catalog JSON and the speaker lists
 * stored on [dev.ahmedmohamed.hayaitts.data.db.entities.InstalledVoiceEntity].
 *
 * - `ignoreUnknownKeys`: tolerate forward-compat fields the catalog adds.
 * - `encodeDefaults`: keep nulls in the encoded form so a missing sha256 is
 *   visible at the wire level.
 */
internal val catalogJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = true
}
