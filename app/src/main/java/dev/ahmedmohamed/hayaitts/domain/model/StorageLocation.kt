package dev.ahmedmohamed.hayaitts.domain.model

/**
 * Where on-device voice bundles are installed. Phase 4a only honours
 * [INTERNAL] — Phase 7 polish actually wires [EXTERNAL] through to the
 * downloader and the runtime path resolver.
 */
enum class StorageLocation {
    INTERNAL,
    EXTERNAL,
}
