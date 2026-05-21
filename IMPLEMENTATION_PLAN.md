# HayaiTTS implementation plan

Historical reference. Reflects the actual state of `main`. Use `HANDOFF.md` for the current state and what's next.

## Goal

Premium, Material 3 Expressive Android app that acts as a system-wide TTS engine: many TTS model families (VITS / Piper / Matcha / Kokoro / Kitten / ZipVoice / Pocket / Supertonic), per-voice tuning, hardware acceleration, downloads manager with extraction progress, in-app speaker picker.

## Phase status

| Phase | Description | Status |
|-------|-------------|--------|
| 1. Research & setup | Audit existing code, dep stack, pick versions. | Done |
| 2. Core UI redesign | Rework Home/Browse/Playground/Settings/Downloads on M3 Expressive containers, top app bars, list groups, animated transitions. | Done (shipped v1.1.0 + v1.1.1, polished further in `36101e8`) |
| 3. Performance & Hardware settings | NNAPI toggle, thread count, max-sentences-per-call. Engine cache must rebuild on change. | Done (`5891c2c`) |
| 4. Model management & speaker picker | Per-voice tuning controls (incl. noiseScale / noiseScaleW for VITS). In-app default-speaker picker. Speakers exposed to system TTS. | Done (`5891c2c` for tuning + system TTS exposure, `d41d40a` for SpeakerPickerActivity) |
| 5. Downloads manager | Queue UI, real extraction progress with percentage, system notifications, cancel. | Done (`7726b67` original, `682acf7` extraction progress) |
| 6. Filters & search | Accurate browse filters, active-filter chips, language matching. | Done (shipped v1.1.1, leading dots + error-tinted reset added in `36101e8`) |
| 7. System integration | Manifest registration, expose individual speakers via `TextToSpeech.Engine`, route default speaker through `onSynthesizeText`. | Done (manifest already wired pre-v1.1; speaker exposure + default routing in `5891c2c` / `d41d40a`) |
| 8. Testing & QA | JVM unit tests + instrumented tests. | Partial — JVM units shipped (`70adc0f`), androidTest scaffold wired in Gradle but no instrumented suites yet. |
| 9. Release prep | Update channel, changelog, Play Store. | Out of scope per user direction. |

## Architecture snapshot

See `HANDOFF.md` → "Architecture snapshot". Critical files:

- `data/tts/SherpaTtsRuntime.kt` — engine LRU + cache key.
- `tts/HayaiTtsService.kt` — system TTS entry, `onGetVoices`, `onSynthesizeText`.
- `data/settings/SettingsRepositoryImpl.kt` — DataStore-backed preferences.
- `domain/model/DownloadState.kt` — sealed download FSM.
- `data/download/VoiceDownloadWorker.kt` — CoroutineWorker + extraction.
- `ui/theme/{Theme,Type,Color}.kt` — design system.
- `ui/speaker/SpeakerPickerActivity.kt` — in-app speaker picker.

## Verification

```powershell
.\gradlew assembleDebug
.\gradlew lint
.\gradlew test
```

End-to-end manual checks live in `HANDOFF.md` → "Verification checklist".

## Original open questions, resolved

- **minSdk**: 26 (set in `app/build.gradle.kts`). NNAPI fall-through to CPU is handled by sherpa-onnx so older devices simply ignore the toggle.
- **Default thread / sentence limit**: 2 threads, 2 sentences (matches sherpa-onnx upstream defaults).
- **Theme**: Voice Teal custom palette with light + dark variants, `MaterialExpressiveTheme` with `MotionScheme.expressive()`. Dynamic color is not opted into.
- **Bundled models**: only the Amy / Piper voice is bundled. Everything else is downloaded on demand from the catalog.
