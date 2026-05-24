package dev.ahmedmohamed.hayaitts.ui.theme

import androidx.compose.ui.graphics.Color

// HayaiTTS is a flat-monochrome app. Every M3 ColorScheme slot is bound to a
// step on this neutral ramp in Theme.kt — we never let MaterialTheme fall
// back to expressiveLightColorScheme()/darkColorScheme() defaults, because
// those defaults include tinted greens (tertiary*) and blues
// (secondaryContainer*) that violate the design contract.
//
// Ramp: white-to-black in ~10-point increments. Steps map to a roughly even
// perceptual gradient under sRGB display gamma; they are not strict
// HCT tones but render close enough on AMOLED + LCD that no slot looks
// out of place beside another.
val Neutral00 = Color(0xFFFFFFFF) // pure white — light surfaceBright extreme
val Neutral05 = Color(0xFFFAFAFA) // light background
val Neutral10 = Color(0xFFF2F2F2) // light surfaceContainerLowest
val Neutral15 = Color(0xFFEAEAEA) // light surfaceContainerLow
val Neutral20 = Color(0xFFE0E0E0) // light surfaceContainer / outlineVariant
val Neutral25 = Color(0xFFD2D2D2) // light surfaceContainerHigh
val Neutral30 = Color(0xFFBDBDBD) // light surfaceContainerHighest / outline
val Neutral40 = Color(0xFF8E8E8E)
val Neutral50 = Color(0xFF6E6E6E) // light secondary
val Neutral60 = Color(0xFF555555)
val Neutral70 = Color(0xFF3A3A3A)
val Neutral80 = Color(0xFF2A2A2A) // dark surfaceContainerHighest
val Neutral85 = Color(0xFF222222) // dark surfaceContainerHigh
val Neutral90 = Color(0xFF181818) // dark surfaceContainer
val Neutral92 = Color(0xFF141414) // dark surfaceContainerLow
val Neutral95 = Color(0xFF101010) // dark background / surface
val Neutral100 = Color(0xFF000000) // pure black

// Semantic — error is the only allowed accent in the whole app.
val ErrorLight = Color(0xFFB3261E)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFF9DEDC)
val OnErrorContainerLight = Color(0xFF410E0B)

val ErrorDark = Color(0xFFF2B8B5)
val OnErrorDark = Color(0xFF601410)
val ErrorContainerDark = Color(0xFF8C1D18)
val OnErrorContainerDark = Color(0xFFF9DEDC)
