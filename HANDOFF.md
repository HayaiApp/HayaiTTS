# Handoff

Entry point for the next agent. Reflects state as of the v1.1.2 work landed in `main`.

## What shipped recently (post v1.1.1)

| Area | Commit | Summary |
|------|--------|---------|
| Theme typography | `99dd1b4` | Outfit (display/headlines) + Inter (body/labels) via Compose Downloadable Fonts. AGP 8.8.2, material3 1.5.0-alpha20. |
| Performance & Hardware | `5891c2c` | Settings UI for NNAPI / threads / max-sentences. SherpaTtsRuntime engine cache key now folds all three plus per-voice noiseScale/noiseScaleW. HayaiTtsService exposes `voiceId#speakerId#name` framework Voice entries to the OS. |
| Downloads | `682acf7` | `VoiceDownloadWorker` reports real extraction progress (CountingInputStream over bz2) on a 5%/250 ms throttle and honors `isStopped`. `DownloadState.Extracting` carries the fraction. |
| In-app speaker picker | `d41d40a` | New `SpeakerPickerActivity` + `SettingsRepository.defaultSpeakerByVoice` (JSON-encoded map). Reached from Library card overflow and VoiceDetail. The TTS service consults the default speaker when the framework doesn't supply one. |
| Expressive redesign | `36101e8` | Onboarding (per-page accent + hero shape + parallax), Help (color+shape morph on expand), Downloads (animated container per state, tonal section dots, extraction-pct progress), Browse filter sheet (leading dots + error-tinted Reset), CustomImport (per-phase tonal PhasePanel). Two latent `DownloadState.Extracting` callers updated to `is`-pattern. |
| Baseline tests | `70adc0f` | JVM unit tests for VoiceTuning, DownloadState, SpeakerPickerActivity intent extras. Gradle wiring for compose-ui-test + androidx.test.* (instrumented tests deferred). |

## Architecture snapshot

- **Engine cache**: `data/tts/SherpaTtsRuntime.kt` — keyed by `(voiceId, lengthBucket, noiseBucket, noiseWBucket, useNnapi, numThreads, maxNumSentences)`. Reads `SettingsRepository` flows synchronously via Koin `GlobalContext` at each `synthesize()` call. Bucketed so a slider bounce does not churn native handles.
- **System TTS entry**: `tts/HayaiTtsService.kt`. `onGetVoices()` expands multi-speaker voices into `voiceId#speakerId#name` framework `Voice`s. `onSynthesizeText()` consults, in order: request voice name → last selected → `SettingsRepository.defaultSpeakerByVoice[voiceId]` → first speaker.
- **Settings source of truth**: `data/settings/SettingsRepositoryImpl.kt`. DataStore Preferences. New keys: `use_nnapi`, `synthesis_threads`, `max_num_sentences`, `default_speakers_json`.
- **Downloads**: `data/download/VoiceDownloadWorker.kt` is a `CoroutineWorker`. State lives in Room (`DownloadStateEntity`) and projects to `DownloadState` for the UI. Active downloads run as a foreground service via `setForegroundSafely`; cancel via `WorkManager.cancelUniqueWork(uniqueName(voiceId))`.
- **Design system**: `ui/theme/Theme.kt` (`MaterialExpressiveTheme` + `MotionScheme.expressive()`), `ui/theme/Type.kt` (downloadable fonts), `ui/theme/Color.kt` (Voice Teal palette + dark variant), `res/values/font_certs.xml` (Google Fonts provider certs).
- **Reusable card components**: `ui/components/VoiceCard.kt` (Installed / Catalog / Featured variants), `ui/components/EmptyState.kt`, `ui/components/DownloadProgress.kt`.

## What's next

Nothing on the original revamp roadmap is still pending — everything except Phase 9 (release prep) has shipped, and Phase 9 is out of scope per user direction. Pick up new work from issues / user requests.

Known small-scope opportunities (only if asked):

- Instrumented tests are wired in Gradle but no `androidTest/` sources exist yet. Adding a Compose UI test for `SettingsActivity` (toggle NNAPI + assert persisted) and `SpeakerPickerActivity` (launch with multi-speaker voice intent + assert result) would lock in the highest-risk paths.
- `defaultSpeakerByVoice` is read on every `onSynthesizeText` call. If profiling shows the DataStore read is a hot path, cache the latest value via a hot `StateFlow` in `SherpaTtsRuntime`.
- `androidx.compose.material3.MaterialShapes` (alpha20) is not used anywhere yet. The onboarding hero shape and CustomImport phase panels both use `RoundedCornerShape` with asymmetric corners; they could be promoted to real polygon morphs once `MaterialShapes.toShape()` is stable.

## Verification checklist

```powershell
.\gradlew assembleDebug
.\gradlew lint
.\gradlew test
```

Manual sanity (emulator API 33+):

- Onboarding: 4 pages each with distinct accent + hero shape; chunky pill indicator stretches into active page color.
- Settings → Performance & Hardware: toggle NNAPI, change thread count + sentence limit, kill + relaunch → values persist.
- Library → multi-speaker voice → overflow → "Choose default speaker…" → pick speaker. Relaunch → speaker is the default in Playground and surfaces correctly via Android system TTS.
- VoiceDetail → "Choose default speaker…" text button next to the speakers row launches the same picker.
- Trigger a voice download → DownloadsScreen card animates to tertiaryContainer, wavy progress reports download %, then extraction %, then settles back to surfaceContainer when done.
- Help screen → tap an expandable card → container morphs from surfaceContainerHighest to primaryContainer with the bottom corners going asymmetric.
- Browse → open filter sheet → each filter group has a tonal leading dot; "Reset" text button is error-tinted.
- Custom import: pick a tar.bz2 → Analyzing panel renders on tertiaryContainer with an asymmetric cookie shape; Importing panel rotates container + shape per step (Copying / Extracting / Validating).
