# Changelog

All notable changes to HayaiTTS land here. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

## [v1.1.1] — 2026-05-17

### Changed

- **Browse search bar** is now a plain inline field that just filters the
  list below — no more "expanded" state stacking duplicate voice cards
  inside the bar.
- **Voice card download UI** consolidated to one indicator. The circular
  wavy progress that wrapped the family badge during downloads is gone;
  the inline `LinearWavyProgressIndicator` under the chip strip is the
  sole progress surface. Cards are shorter and cleaner.
- **VoiceDetail toolbar** holds every secondary action: Quick Switch lives
  in the bar's actions slot and a 3-dot overflow exposes Playground and
  Uninstall. The screen body now has exactly one primary CTA (Install /
  Cancel / per-locale default chips), with download progress rendered
  above the button instead of beside scattered controls.

### Added

- **Settings entry on the Library top bar** — gear icon launches
  `SettingsActivity` directly. Settings was previously only reachable
  from the system TTS engine ⚙ button, which most users never see.
- **Downloads entry on the Library top bar** — cloud-download icon with
  a count badge for active downloads, opening the Downloads Manager.
- **Gender filter on Browse** — the filter sheet now has a Voice type
  group (Female / Male / Unspecified) backed by the catalog's per-
  speaker `gender` field. Active selections render as removable chips
  alongside the existing tier / language / family chips.

## [v1.1.0] — 2026-05-17

### Added

- **In-app auto-updater** — Settings → Updates picks a release channel
  (Stable / Beta / Nightly), polls the GitHub Releases API on launch
  (debounced 6 h), surfaces an M3 dialog with the changelog body, and
  streams the universal APK through a `FileProvider`-backed installer.
- **Downloads Manager** — dedicated screen reachable from a badged top-bar
  icon on Library, showing Active / Failed / Recently completed
  downloads with per-row Cancel, Retry, Remove, and Clear-completed
  actions. Two-channel notifications (`downloads_active` foreground,
  `downloads_complete` events) with M3 progress, big-text style, and
  Cancel/Retry PendingIntents.
- **Model Playground** — per-voice page with pitch / speed / length
  sliders, multi-shot generation history (last 10 samples persisted to a
  new Room v3 `playground_samples` table), live 32-bin waveform during
  playback, multi-speaker swap. Pitch implemented as a post-process
  resample so it works on every model family.
- **First-launch onboarding** — 4-card flow on first install with a
  hardware-tier recommendation ("Mid models balance speed and quality on
  your device") and a deep link to system TTS settings.
- **Help screen** — Settings → Help opens expandable M3 cards covering
  getting-started, tier selection, custom imports, storage location,
  update channels, and troubleshooting recipes.
- **M3 RichTooltips** anchored on Library Import, Browse Filter,
  VoiceDetail Play, and the QuickSwitcher entry. Trigger on long-press,
  dismiss on outside tap.
- **Localization** to 10 languages — Spanish, French, German, Italian,
  Portuguese (Brazil), Russian, Japanese, Chinese (Simplified), Korean,
  and **Arabic with RTL** layout. ~80 idiomatic strings per locale.

### Changed

- **Flat M3 Expressive surfaces everywhere** — every voice card, hero,
  badge, and avatar now uses `MaterialTheme.colorScheme.*` exclusively.
  No more per-family gradients or hand-picked seed colors. Family
  identity lives in the icon glyph + family chip, not the background.
- **Browse rebuilt** with a `DockedSearchBar` floating at the top, a
  single Filters icon (with a badge for active-filter count) opening a
  grouped bottom sheet (Tier / Language / Family with multi-select +
  segmented Tier), and an active-filter chip row with per-chip remove
  affordances.
- **Piper Amy unbundled** — first-run no longer ships a 78 MB voice
  asset. Browse handles voice acquisition. Per-ABI release APK drops
  from 96–112 MB to **28–44 MB** (−64% on arm64-v8a).

### Infrastructure

- `catalog-refresh.yml` now commits the catalog straight to `main`
  rather than opening a PR (Actions can't open PRs by default and the
  scraped catalog has no human-review value).
- Catalog generator picks up the three new SOTA families by substring
  (`zipvoice`, `pocket-tts`, `supertonic`).

## [v1.0.0] — 2026-05-17

First stable release. Same surface as the beta below, plus the
initial signed release pipeline.

## [v1.0.0-b1] — 2026-05-17

First public beta — full feature list documented in the commit
history and the
[release notes](https://github.com/HayaiApp/HayaiTTS/releases/tag/v1.0.0-b1).

[Unreleased]: https://github.com/HayaiApp/HayaiTTS/compare/v1.1.1...HEAD
[v1.1.1]: https://github.com/HayaiApp/HayaiTTS/releases/tag/v1.1.1
[v1.1.0]: https://github.com/HayaiApp/HayaiTTS/releases/tag/v1.1.0
[v1.0.0]: https://github.com/HayaiApp/HayaiTTS/releases/tag/v1.0.0
[v1.0.0-b1]: https://github.com/HayaiApp/HayaiTTS/releases/tag/v1.0.0-b1
