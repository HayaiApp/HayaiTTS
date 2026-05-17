# Changelog

All notable changes to HayaiTTS land here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); we don't use
strict semver yet (1.0.0 is the first public beta).

## [Unreleased]

- ZipVoice + Pocket families pick up their upstream releases on the next
  weekly `catalog-refresh` run (slug matcher fixed in `a9f8a97`).

## [v1.0.0-b1] — 2026-05-17

### Added — first public beta

- **System-wide TTS engine** — registers in *Settings → System →
  Languages & input → Text-to-speech engines*, speakable by any app.
- **Bundled Piper Amy** (`en_US-amy-low`) voice plays out of the box,
  before any downloads.
- **186-voice catalog** of sherpa-onnx-published bundles, each verified
  by SHA-256 captured during the catalog scrape. Five families
  represented at this build: Piper (172), Kitten (7), Kokoro (3),
  Matcha (3), Supertonic (1). Three more (VITS, ZipVoice, Pocket) are
  runtime-wired and will populate on the next catalog refresh.
- **All 8 sherpa-onnx families wired in `SherpaTtsRuntime`** —
  per-family `OfflineTtsConfig` builders for Piper / VITS / Matcha /
  Kokoro / Kitten / ZipVoice / Pocket / Supertonic.
- **Material 3 Expressive UI** — `MaterialExpressiveTheme`,
  `MotionScheme.expressive()`, `WavyProgressIndicator`,
  `LargeFlexibleTopAppBar`, `MultiChoiceSegmentedButtonRow`,
  `HorizontalFloatingToolbar`, `MaterialShapes`-driven badge morphing.
- **Global voice quick-switcher** — bottom sheet reachable from any
  screen's top bar lists installed voices grouped by language. Tap to
  set the default for that locale, with haptic feedback.
- **Library**: featured carousel, favorites (star toggle), long-press
  drag-reorder persisted to DataStore.
- **Browse**: family-tinted gradient cards, segmented-button tier
  filter, multi-select language + family filters with animated count
  badges, container-transform-style nav to detail.
- **Voice Detail**: immersive hero with family icon + colors, 32-bin
  live waveform driven by AudioTrack amplitudes during preview, speaker
  selection with circular avatars.
- **Custom voice import** — SAF picker accepts `.tar.bz2`, `.tar`,
  `.zip`, or bare `.onnx`. Streaming analyzer captures inline metadata
  files (speakers.txt, voices.txt, JSON manifests) for real
  multi-speaker detection.
- **Per-locale defaults** — `DefaultsRepository` + Settings UI; the
  TTS service routes synthesis by the request locale.
- **Storage location move** — toggle internal ⇄ SD card and the
  migrator physically relocates every installed voice with progress UI
  (not just persists the preference).
- **R8 minify + per-ABI splits** — release APKs are 96–112 MB per ABI
  vs. 170 MB universal.
- **Apksigner-direct signing in CI** — releases signed with a real
  RSA-4096 keystore stored in encrypted GitHub secrets, verified post-
  sign with `apksigner verify`.

### Infrastructure

- **Blacksmith** runners for CPU-bound builds, GitHub-hosted runners
  for bandwidth-bound catalog refresh.
- **`catalog-refresh.yml`** runs weekly + on dispatch; commits the new
  catalog directly to `main` (no PR step).
- **`build_push.yml`** publishes drafts for `v*` tags and auto-publishes
  nightlies for `r*` tags in the same repo, all prerelease-flagged.

[Unreleased]: https://github.com/HayaiApp/HayaiTTS/compare/v1.0.0-b1...HEAD
[v1.0.0-b1]: https://github.com/HayaiApp/HayaiTTS/releases/tag/v1.0.0-b1
