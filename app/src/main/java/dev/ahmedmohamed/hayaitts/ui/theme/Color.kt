package dev.ahmedmohamed.hayaitts.ui.theme

import androidx.compose.ui.graphics.Color

// HayaiTTS v2 ships a **flat monochrome** palette. The launcher icon still
// uses the original voice teal (see .github/branding/app-icon.svg) for brand
// identity, but every in-app surface lives on a neutral grayscale grid so
// the UI reads as type+space first, with no color carrying meaning.
//
// Naming kept as `VoiceTeal*` for source-compatibility with the existing
// Theme.kt wiring. The values are now near-greys.
val VoiceTeal = Color(0xFF1F1F1F)

// LIGHT — surfaces sit on near-white; primary is near-black for high contrast
// type and key controls.
val VoiceTealLightPrimary = Color(0xFF1A1A1A)
val VoiceTealLightOnPrimary = Color(0xFFFFFFFF)
val VoiceTealLightPrimaryContainer = Color(0xFFE6E6E6)
val VoiceTealLightOnPrimaryContainer = Color(0xFF111111)
val VoiceTealLightSecondary = Color(0xFF555555)
val VoiceTealLightBackground = Color(0xFFFAFAFA)
val VoiceTealLightOnBackground = Color(0xFF101010)

// DARK — surfaces near-black, primary near-white.
val VoiceTealDarkPrimary = Color(0xFFE8E8E8)
val VoiceTealDarkOnPrimary = Color(0xFF101010)
val VoiceTealDarkPrimaryContainer = Color(0xFF2A2A2A)
val VoiceTealDarkOnPrimaryContainer = Color(0xFFE8E8E8)
val VoiceTealDarkSecondary = Color(0xFFB0B0B0)
val VoiceTealDarkBackground = Color(0xFF101010)
val VoiceTealDarkOnBackground = Color(0xFFE8E8E8)
