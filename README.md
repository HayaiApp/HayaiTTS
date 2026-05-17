<div align="center">

# HayaiTTS

**Offline neural text-to-speech for Android, with Material 3 Expressive.**

[![CI](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_check.yml/badge.svg)](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_check.yml)
[![Release](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_push.yml/badge.svg)](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_push.yml)
[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/HayaiApp/HayaiTTS?include_prereleases)](https://github.com/HayaiApp/HayaiTTS/releases)

</div>

HayaiTTS registers itself as a system-wide text-to-speech engine on Android,
so any app that talks via `android.speech.tts.TextToSpeech` — including the
[Hayai](https://github.com/HayaiApp/hayai) novel reader — speaks with neural
voices instead of the stock Google or Samsung engine.

Everything runs on-device. No accounts, no cloud, no network during
synthesis.

## Highlights

- **System TTS engine** — appears in *Settings → System → Languages & input
  → Text-to-speech engines*. Pick HayaiTTS, set it as the default engine,
  and every other app on the device now uses neural voices.
- **186-voice catalog** scraped weekly from the upstream
  [k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) model index,
  covering 8 model families:
  - **Piper** (172 voices, 40+ languages — small, fast, MIT)
  - **Kokoro 82M** (3 multilingual voices — SOTA quality)
  - **Kitten** (7 nano-tier English voices — fastest)
  - **Matcha-TTS** (3 diffusion voices — very high quality)
  - **Supertonic** (1 voice — newest 2026 model)
  - **ZipVoice** + **Pocket** (placeholders — will populate on the next
    catalog refresh after the slug-matcher fix lands)
  - **VITS** classics (engine wired; voices appear as upstream re-uploads)
- **Bundled-in-APK Piper Amy** so the app speaks the moment it's
  installed, before any downloads.
- **Material 3 Expressive** end-to-end: `MaterialExpressiveTheme`,
  `MotionScheme.expressive()` springs, `WavyProgressIndicator`,
  `LargeFlexibleTopAppBar`, `MultiChoiceSegmentedButtonRow`,
  `MaterialShapes` morphing, container-transform-style nav.
- **Quick voice switcher** — a global bottom sheet (top-bar icon on every
  screen) lists installed voices grouped by language. Tap to set the
  default for that locale, with haptic feedback.
- **Featured + favorites + reorder** — pinned voices float to the top of
  the Library, long-press a card to drag-reorder, star toggles favorites.
- **Per-family visual identity** — every voice card carries a gradient
  tinted by the family's seed color (Piper=teal, Kokoro=violet,
  Kitten=amber, VITS=cyan, Matcha=green, ZipVoice=indigo, Pocket=rose,
  Supertonic=gold). The family badge morphs between rounded and polygon
  shapes during downloads.
- **Live waveform preview** — Voice Detail's Play button feeds a 32-bin
  amplitude-driven `LinearWavyProgressIndicator` so you actually see the
  audio.
- **Custom voice import** — pick any sherpa-onnx-compatible bundle
  (`.tar.bz2`, `.tar`, `.zip`, or a bare `.onnx`) via SAF. The analyzer
  detects the family from the file layout, parses any `speakers.txt` /
  `speaker_id_map` JSON for real multi-speaker support, lets you confirm
  or override, then installs into the engine's voice library.
- **Per-locale defaults** — set a different voice for English vs.
  Japanese vs. Chinese; the TTS service routes synthesis requests to the
  right model based on the locale Android passes in.
- **Storage location toggle** — internal storage or external SD card.
  When you flip the preference, every installed voice is physically
  relocated with progress UI (not just persisted).
- **R8 + per-ABI splits** — release APKs land at 96–112 MB per ABI vs.
  170 MB for the universal build.
- **Signed releases** via `apksigner` running in GitHub Actions against
  a real RSA 4096 keystore stored in encrypted repo secrets.

## Install

Latest **stable / beta APKs**:
**[github.com/HayaiApp/HayaiTTS/releases](https://github.com/HayaiApp/HayaiTTS/releases)**

After install:

1. Open the app once so it can mirror the bundled Amy voice into private
   storage.
2. Go to *Settings → System → Languages & input → Text-to-speech engines*
   (the path varies by Android skin — look for "Preferred engine").
3. Pick **HayaiTTS**. Hit "Listen to an example" to verify Amy speaks.
4. Open HayaiTTS, tap the **Browse** FAB, install whichever neural voices
   you want.

Hayai's novel reader settings auto-detect HayaiTTS once installed and
surface a "Manage neural voices" button that deep-links straight to the
Library screen.

## Build from source

You'll need:

- **JDK 21** — the JBR that ships with Android Studio works.
- **Android SDK** with platform 36 + build-tools 36.0.0.
- **Git LFS** — the bundled Piper voice (28 MB `.onnx`) and the
  vendored `sherpa-onnx-1.13.2.aar` (55 MB) are LFS-tracked.

```bash
git clone https://github.com/HayaiApp/HayaiTTS.git
cd HayaiTTS
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows, point Gradle at the JBR explicitly so it doesn't pick up a
random JRE from `PATH`:

```powershell
.\gradlew assembleDebug `
  -Dorg.gradle.java.home="C:\Program Files\Android\Android Studio\jbr"
```

For signed release builds, see [ONBOARDING.md](ONBOARDING.md#release-signing).

## Release streams

| Stream | Tag pattern | Channel |
|---|---|---|
| **Stable** | `v1.0.0` | [Releases](https://github.com/HayaiApp/HayaiTTS/releases?q=prerelease%3Afalse) |
| **Beta** | `v1.0.0-b1` | [Releases (prerelease)](https://github.com/HayaiApp/HayaiTTS/releases?q=prerelease%3Atrue+v) |
| **Nightly** | `r17` | [Releases (prerelease)](https://github.com/HayaiApp/HayaiTTS/releases?q=prerelease%3Atrue+r) — every push to `main` |

In-app auto-updaters can discriminate cleanly: tags starting with `v`
are stable/beta (look for the `-b` suffix); tags starting with `r` are
nightlies; all betas and nightlies carry the GitHub `prerelease=true`
flag.

## Catalog

The voice catalog (`catalog/v1/models.json`) is regenerated by
[`tools/catalog/build_catalog.py`](tools/catalog/build_catalog.py),
which scrapes the
[sherpa-onnx TTS model index](https://k2-fsa.github.io/sherpa/onnx/tts/all/),
streams every release tarball through SHA-256 to compute integrity
hashes, and emits a deterministic JSON file. The
[`catalog-refresh`](.github/workflows/catalog-refresh.yml) workflow runs
the script every Monday on a free GitHub-hosted runner and commits the
new catalog directly to `main`.

Voice bundles aren't on every release tarball's allow-list yet — when
the runtime can't synthesize a family (because sherpa-onnx hasn't
shipped the JNI for it), the catalog flags those entries
`available: false` and the Browse screen surfaces them as
"Coming Soon".

## Licensing

The app itself is **GPL-3.0** — see [LICENSE](LICENSE).

Per-voice licenses vary and are captured in each catalog entry's
`license` field. The bundled Piper Amy voice is **MIT** upstream and is
redistributed under the same terms. Check the upstream source on
Hugging Face / GitHub before redistributing any individual voice
commercially.

## Related

- **[Hayai](https://github.com/HayaiApp/hayai)** — the manga / novel
  reader that surfaces HayaiTTS as a TTS engine in its novel reader
  settings.
- **[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)** (Apache-2.0)
  — the on-device inference runtime.
- **[Piper voices](https://huggingface.co/rhasspy/piper-voices)** — the
  upstream release artifacts for the Piper family.

## Contributing

PRs welcome. The [`build_check`](.github/workflows/build_check.yml)
workflow validates every PR against `assembleRelease` + `assembleDebug`
on Blacksmith. For UI changes, please attach a screenshot or short
screen recording — Compose previews and unit tests don't capture
motion/haptics, which is where most of our UX value lives.

For architecture, phase-by-phase history, the runtime smoke-test plan,
and known limits, see [ONBOARDING.md](ONBOARDING.md).
