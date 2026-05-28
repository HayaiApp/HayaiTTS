<div align="center">

# HayaiTTS

**Offline, on-device neural text-to-speech for Android.**

[![CI](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_check.yml/badge.svg)](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_check.yml)
[![Release](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_push.yml/badge.svg)](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_push.yml)
[![Catalog refresh](https://github.com/HayaiApp/HayaiTTS/actions/workflows/catalog-refresh.yml/badge.svg)](https://github.com/HayaiApp/HayaiTTS/actions/workflows/catalog-refresh.yml)
[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/HayaiApp/HayaiTTS?include_prereleases)](https://github.com/HayaiApp/HayaiTTS/releases)

</div>

A drop-in replacement for the stock Android TTS engine. Synthesis happens
locally on the phone via [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)
&mdash; no network, no account, no telemetry. Every app on the device that
calls into `TextToSpeech` (TalkBack, navigation, reader apps, the browser's
read-aloud) speaks with whichever voice you install.

- **188 voices** across 5–7 model families, **73 languages**. Full
  index at [`docs/MODELS.md`](docs/MODELS.md) (auto-regenerated weekly).
- **In-app catalog browser** with per-(speaker, language) audition clips
  rendered offline by `tools/samples/render_samples.py`, hosted at
  [HayaiTTS-samples](https://github.com/HayaiApp/HayaiTTS-samples).
- **Streaming-capable runtime** — `SherpaTtsRuntime.synthesizeStreaming` and
  `synthesizeCloned` are wired up; the cloning UI is in active development.
- **System TTS engine**, `MANAGE_VOICES` activity-alias for system settings
  cog, foreground-service downloader, in-app update channel with auto-poll.

---

## Install

1. Grab the universal `*.apk` from the [latest release](https://github.com/HayaiApp/HayaiTTS/releases)
   (`hayaitts-vX.Y.Z.apk`). Sideload it &mdash; the Play Store doesn't permit
   apps that ship GPL-3 binaries in this configuration.
2. Open the app. The bundled English voice (Piper Amy) ships inside the APK
   so first-run synthesis works offline immediately. Browse to download more.
3. *Settings → System → Languages & input → Text-to-speech* → **HayaiTTS**.
   The exact path varies by OEM (Samsung One UI calls it "Speech");
   `MANAGE_VOICES` activity-alias is wired in the manifest so the cog
   next to HayaiTTS on that page lands you on Library directly.

Update channels (Stable / Beta / Nightly) are switched from
*Settings → Update channel*; nightlies publish to a separate
[HayaiApp/HayaiTTS-nightly](https://github.com/HayaiApp/HayaiTTS-nightly)
repo so the main Releases page stays focused on production builds.

---

## Features

### Catalog & install
- 188 voices spanning Piper, Kokoro, Kitten, Matcha, Supertonic
  (ZipVoice and Pocket families wired in the runtime; awaiting upstream
  model releases — see [`docs/MODELS.md`](docs/MODELS.md))
- Catalog auto-refreshed weekly from the
  [sherpa-onnx TTS model index](https://k2-fsa.github.io/sherpa/onnx/tts/all-pretrained-models.html);
  see [`catalog-refresh.yml`](.github/workflows/catalog-refresh.yml).
- Per-voice sha256 verification on download; resumable downloads via
  WorkManager with backoff.
- WiFi-only download toggle, configurable storage location (internal /
  external SD), live storage-migrator for moving installed voices between
  locations without losing them.
- Custom voice import: sherpa-onnx-compatible `.tar.bz2` bundles can be
  installed from disk via the SAF import flow.

### Browse + filters
- Filter by tier (low / mid / high), gender, language, model family,
  capability (e.g. "Voice cloning"). All filters compose; result count
  reflects the active filter set.
- Inline search field in the floating top bar, plus dedicated search for
  the long languages list inside the filter sheet.
- **Global allowed-languages setting** in Settings → Defaults gates the
  whole catalog so users with a narrow language interest never see the
  other 60+ tags.
- Per-(speaker, language) audition: tap play on any catalog row to hear
  a 5-second clip before downloading.

### Per-voice surfaces
- **Voice Detail**: hero card, license, sample-rate / family / tier chip
  strip, speaker picker (avatars for multi-speaker models, friendly
  fallback labels for anonymous `speaker_N` placeholders).
- **Studio (Playground)**: live speed / pitch / length / noise / noise-W
  sliders, persisted per voice. Waveform amplitude indicator, sample
  history with delete + replay. In-bar voice picker so users tune
  multiple voices without leaving the tab.
- **Default voice per locale**: each installed voice can be pinned as the
  default for any locale it ships. The TTS engine routes by locale at
  synthesis time.

### Activity tab
- Live download / extraction progress (determinate, matches the rest of
  the app).
- Synthesis telemetry: RTF, synth ms, audio ms, char count, caller package
  per request. Filterable.
- Cache + storage stats.

### Quality of life
- M3 Expressive theme, monochrome by design, dynamic-color opted out.
- Floating-pill top bar that doesn't collapse on scroll &mdash; content
  scrolls under the bar; status-bar inset handled once at the bar level.
- M3 navigation bar with five tabs; bottom-nav insets correctly applied
  so content never gets cut off by the system navigation.
- Bottom-sheet quick switcher (`HayaiQuickSwitcher`) hoisted at the
  activity root so every screen can flip the default voice with one tap.
- Auto-updater: 6 h debounce on launch-time auto-check; manual
  "Check for updates" in Settings; in-app APK install via FileProvider.
- **Crash reporter**: an `UncaughtExceptionHandler` redirects to a
  `:crash` process activity that auto-copies the stack trace + device
  metadata to clipboard for paste into a GitHub issue.

### Privacy
- No network during synthesis. The only network calls are:
  - Catalog JSON refresh (one `raw.githubusercontent.com` GET).
  - The voice bundle download you explicitly trigger.
  - The audition MP3 streamed from the samples repo when you tap play.
- No analytics, no crash reporting service, no accounts.
- All voice data lives in `filesDir/voices/` (or external SD if selected).

---

## Voice catalog

The catalog is the source of truth for everything Browse shows.

- Machine-readable: [`catalog/v1/models.json`](catalog/v1/models.json)
  — one `VoiceCard` per entry, fields match the Kotlin
  [`VoiceCard`](app/src/main/java/dev/ahmedmohamed/hayaitts/domain/model/VoiceCard.kt)
  data class exactly so the on-device JSON parser is a single
  `Json.decodeFromString`.
- Human-readable: [`docs/MODELS.md`](docs/MODELS.md) — auto-regenerated
  from the JSON on every catalog refresh (see
  [`tools/catalog/build_model_list.py`](tools/catalog/build_model_list.py)).
- Refresh cadence: weekly, every Monday 06:30 UTC. Hand-trigger via
  `Actions → catalog-refresh → Run workflow`.

| Family | Voices | Cloning | Notes |
|---|---:|:-:|---|
| Piper | 174 | — | VITS, 10–60 MB, ~70 languages |
| Kokoro | 3 | — | Higher-quality VITS, 80–360 MB, multi-speaker |
| Kitten | 7 | — | Tiny English-only, fastest |
| Matcha | 3 | — | Diffusion + side-vocoder |
| Supertonic | 1 | — | 2026 model, 30 langs × 10 speakers in one bundle |
| ZipVoice | 0 | ✓ | Reference-audio cloning, awaiting upstream release |
| Pocket | 0 | ✓ | Voice-embedding cloning, awaiting upstream release |

Cloning support means
[`OfflineTts.generateWithConfig`](https://github.com/k2-fsa/sherpa-onnx) is
called with a reference clip + transcript in `GenerationConfig`. The runtime
plumbing (`SherpaTtsRuntime.synthesizeCloned`) is in; the UI screen is on
the roadmap.

---

## Architecture

Built on **Kotlin + Jetpack Compose (M3 Expressive)**, **Koin** for DI,
**Room** for installed-voice state, **WorkManager** for downloads,
**OkHttp** for catalog refresh, and the upstream
[**sherpa-onnx**](https://github.com/k2-fsa/sherpa-onnx) AAR
(vendored at `app/libs/sherpa-onnx-1.13.2.aar`) for the JNI runtime.

```
ui/  ───────► UseCase ──────► Repository ──────► Data source ──────► sherpa-onnx JNI
                                                       └──► Room (installed voices)
                                                       └──► DataStore (settings)
```

Dependency direction is enforced by
[**Konsist**](https://github.com/LemonAppDev/konsist) tests in
`app/src/test/java/.../core/konsist/`; the suite gates CI.

- `domain/` is pure Kotlin. No `android.*`, no `androidx.*` (except
  `androidx.annotation`).
- `data/` may import `domain` but never `ui`.
- `ui/` may import both, plus Compose.
- Threading: every coroutine launch flows through the injected
  [`DispatcherProvider`](app/src/main/java/dev/ahmedmohamed/hayaitts/core/dispatchers/DispatcherProvider.kt);
  importing `kotlinx.coroutines.Dispatchers` outside `core/` is a
  Konsist-blocked violation.
- Errors surface as [`Outcome<T>`](app/src/main/java/dev/ahmedmohamed/hayaitts/core/result/Outcome.kt);
  repositories never throw.
- The TTS engine bar is a single shared composable
  ([`HayaiTopBar`](app/src/main/java/dev/ahmedmohamed/hayaitts/ui/components/HayaiTopBar.kt))
  used by every screen — plain Material `TopAppBar` is forbidden.

Full guide: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). Build commands +
release signing + history: [`ONBOARDING.md`](ONBOARDING.md).

---

## Build

JDK 21 required (Android Studio ships JBR 21). Gradle toolchain is pinned,
so the system JDK version doesn't matter as long as `org.gradle.java.home`
points at a JDK-21 install.

```bash
git clone https://github.com/HayaiApp/HayaiTTS.git
cd HayaiTTS
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

PowerShell on Windows:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew assembleDebug "-Dorg.gradle.java.home=$env:JAVA_HOME"
```

Lint is treated as a hard error, the Konsist architecture suite runs on
`:app:test`, and `lintVitalRelease` runs as part of `assembleRelease` —
keep all three green.

### CI / release pipeline

| Workflow | Trigger | Effect |
|---|---|---|
| [`build_check.yml`](.github/workflows/build_check.yml) | PR / push | Build `assembleDebug` + `assembleRelease`, run Konsist + unit tests |
| [`build_push.yml`](.github/workflows/build_push.yml) | manual `workflow_dispatch` | Signed release. `v…` tags → main repo (stable + beta), `r…` tags → [HayaiApp/HayaiTTS-nightly](https://github.com/HayaiApp/HayaiTTS-nightly) |
| [`catalog-refresh.yml`](.github/workflows/catalog-refresh.yml) | weekly + manual | Re-scrape upstream sherpa-onnx index, regenerate `catalog/v1/models.json` and `docs/MODELS.md`, commit to `main` |
| [`render-samples.yml`](.github/workflows/render-samples.yml) | weekly + manual | Render per-(speaker, language) audition MP3s, publish to the `HayaiTTS-samples` releases |

The Android version baked into the APK comes from the git tag (the
release workflow sets `HAYAITTS_VERSION_NAME=${VERSION_TAG}`), so the
"Current version" row in *Settings → Updates* shows e.g.
`Beta · 2.0.0-b3 (203)` or `Nightly · r142 (142)`.

---

## Roadmap

Tracked in this repo's [issues](https://github.com/HayaiApp/HayaiTTS/issues).
Near-term:

- **Voice cloning UI** (`zipvoice` / `pocket`). Runtime is wired; UI
  needs a mic-record + file-upload screen, reference-transcript field,
  and target-text generate flow.
- **Streaming playback** in `HayaiTtsService`. Currently buffers full
  synthesis before pushing PCM; the streaming runtime API
  (`synthesizeStreaming`) is in place, the service callback wiring is
  the next step.
- **Per-speaker metadata**: most upstream catalogs ship `speaker_0…N`
  with no gender/age/style annotations; ingesting community-curated
  metadata where it exists.
- **More filter dimensions**: voice description, training corpus,
  release year, style tags.

---

## Contributing

PRs welcome. Quick rules:

- For UI changes attach a screenshot or short recording. Compose previews
  and Konsist don't capture motion or haptics, which is where most of the
  UX value lives.
- Strings ship in all 10 supported locales (`res/values-*`). CI does not
  block on missing translations yet, but lint will warn — copy your key
  into every locale file (`ar`, `de`, `es`, `fr`, `it`, `ja`, `ko`,
  `pt-rBR`, `ru`, `zh-rCN`) before opening the PR.
- New configuration options go through DI in `app/di/AppModule.kt` rather
  than direct singletons.
- See [`CLAUDE.md`](CLAUDE.md) for the project's coding conventions and
  forbidden patterns.

## FAQ

**Why isn't it on Google Play?**
Bundling GPL-3 binaries (sherpa-onnx) violates Play's distribution terms
when combined with the app's own GPL-3 licensing. Sideload the APK from
Releases.

**Will it drain my battery?**
Synthesis is sub-second on a 2020+ phone. The engine sleeps between
requests; the foreground-service downloader stops itself once the queue
is empty.

**iOS?**
Not possible — iOS doesn't permit third-party processes to replace
`AVSpeechSynthesizer`. (This is also why HayaiTTS isn't a wrapper around
a cloud TTS API.)

**Where do the voices come from?**
[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) curates them from
upstream projects: [Piper](https://github.com/rhasspy/piper),
[Kokoro-82M](https://huggingface.co/hexgrad/Kokoro-82M),
[Kitten](https://huggingface.co/KittenML/kitten-tts-nano-0.2),
[Matcha-TTS](https://github.com/shivammehta25/Matcha-TTS),
[Supertonic](https://huggingface.co/Supertone/supertonic),
[ZipVoice](https://github.com/k2-fsa/ZipVoice),
[Pocket](https://github.com/csukuangfj/pocket-tts).

**Is the cloning model going to be open?**
ZipVoice and Pocket are research models from k2-fsa; both are
Apache-2-licensed on HuggingFace. Hayai surfaces them once the
`sherpa-onnx-zipvoice-…tar.bz2` releases land on the upstream index.

## License

GPL-3.0 — see [LICENSE](LICENSE). Individual voice licenses vary and are
captured per-entry in the catalog (most are MIT or Apache-2.0). Check the
upstream source on Hugging Face / GitHub before redistributing any
individual voice commercially.

## Related

- **[Hayai](https://github.com/HayaiApp/hayai)** — the manga and novel
  reader that uses HayaiTTS for read-aloud.
- **[HayaiTTS-samples](https://github.com/HayaiApp/HayaiTTS-samples)** —
  per-(speaker, language) audition clips, regenerated weekly.
- **[HayaiTTS-nightly](https://github.com/HayaiApp/HayaiTTS-nightly)** —
  nightly `r…` builds for users on the Nightly update channel.
