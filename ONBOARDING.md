# HayaiTTS — onboarding

This document is for anyone (including future you) who needs to make sense
of the repo's history, build it, test it, and ship it without re-reading
every commit message.

## Build phase history

The project was built in seven phases plus a hardening pass. Each phase is a
single logical commit on `main`.

| # | Commit | What landed |
|---|---|---|
| 1 | (in Hayai repo, see `hayai-phase1-tts-ux.patch`) | Hayai-side: three-axis engine/language/voice TTS settings, "Test voice" button, HayaiTTS handoff card. Captured as a patch — apply with `git apply hayai-phase1-tts-ux.patch` on a clean Hayai working tree. |
| 2 | `5010e3b` + `04c0f00` | Repo bootstrap: gradle, M3 Expressive shell (1.5.0-alpha19), branding fork (Hayai SVGs + speaker-wave glyph + voice teal), 440 Hz `TextToSpeechService` stub registering as a system engine. |
| 3 | `58840b7` | Real synthesis: sherpa-onnx wired in, bundled Piper Amy `en_US-low` voice, multi-buffer 16-bit PCM streaming through `SynthesisCallback`. |
| 4 | `51e065d` + `2bda965` | Catalog + downloader data layer (Room v1, OkHttp + WorkManager + Apache Commons Compress for tar.bz2) and Browse / Voice Detail UI with navigation-compose. |
| 5 | `2449bd3` | Kokoro / VITS / Matcha / Kitten config builders, Matcha vocoder sidecar download, "Coming Soon" badge for un-runtimed families. |
| 6 | `44d78df` | Custom-import wizard (SAF picker → Analyzing → Confirm → Importing → Done), Room v2 migration adding `effectiveFamily`. |
| 7 | `494f166` | Polish: ABI splits, R8, settings UI, tier auto-recommendation, cleanup-on-failure, upstream sherpa-onnx 1.13.2 AAR swap unlocking Kokoro + Kitten. |
| H | TBD | Hardening: catalog generator + sha256 enforcement, release-signing template, CI workflows, README + this file. |

## Build & install

### One-time prereqs

- **JDK 21** — the JBR that ships with Android Studio works (`C:\Program Files\Android\Android Studio\jbr`); Temurin 21 also works.
- **Android SDK** with platform 36 + build-tools 36.0.0.
- **Python 3.12+** if you intend to regenerate the catalog — `pip install -r tools/catalog/requirements.txt`.
- **Git LFS** — the bundled Piper voice (28 MB `.onnx`) and the upstream sherpa-onnx AAR (55 MB) are LFS-tracked.

### Debug builds

```powershell
.\gradlew assembleDebug -Dorg.gradle.java.home="C:\Program Files\Android\Android Studio\jbr"
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release signing

Release builds use R8 and per-ABI splits. The signing key is read from a
gitignored `signing.properties` at the repo root:

```bash
keytool -genkeypair -v \
    -keystore hayaitts-release.jks \
    -keyalg RSA -keysize 4096 -validity 36500 \
    -alias hayaitts -storetype JKS

cp signing.properties.template signing.properties
# edit signing.properties with your store + key passwords
```

Then:

```powershell
.\gradlew assembleRelease -Dorg.gradle.java.home="C:\Program Files\Android\Android Studio\jbr"
```

If `signing.properties` is absent the release build falls back to the debug
key so the `assembleRelease` task itself never breaks — but those APKs are
NOT shippable to a store.

## Runtime smoke test (manual, on a device)

CI verifies the build itself but cannot exercise the TTS service end-to-end —
no Android emulator can confirm "does it actually speak". Run this once per
release on real hardware:

```bash
# 1. Sideload the debug APK.
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. Confirm the engine registered.
adb shell settings list secure | grep -i tts
# Look for a `tts_default_synth` line; the engine package
# `dev.ahmedmohamed.hayaitts` should appear in the engine picker.

# 3. Make HayaiTTS the preferred engine.
adb shell am start -a android.settings.TTS_SETTINGS

# 4. Tap "Listen to an example". You should hear Amy speak the standard
#    "The quick brown fox" line. If you hear a 440 Hz tone instead,
#    sherpa-onnx failed to load — check `adb logcat -s HayaiTtsService`
#    for the asset-mirror copy log lines and the OfflineTts init line.

# 5. From HayaiTTS itself, hit Browse → install Alan en_GB-low (smallest
#    extra voice). Confirm the download progress wavy bar advances, the
#    "Extracting…" indeterminate spinner takes over for ~10 s, and the
#    voice appears in Library with a "Set default" toggle.

# 6. Custom-import: download any other Piper voice's .tar.bz2 to the
#    device, hit Library → Import, pick the file. Confirm the wizard
#    analyzes correctly, lets you confirm, imports, and the voice appears
#    in Library with a "Custom" chip.
```

## Catalog regeneration

```bash
python tools/catalog/build_catalog.py --cache-dir .catalog_cache --output catalog/v1/models.json
```

The first run hashes every voice in the upstream index (~5–10 GB streamed,
30–60 minutes). The cache makes subsequent runs near-instant unless
upstream rebuilds a release tarball, in which case the changed entries
re-hash.

The weekly GitHub Action does the same automatically — if anything changes
upstream, you'll see a `chore/catalog-refresh` PR.

## Known limits

- **Release APK signed with debug key by default**. Replace
  `signing.properties` before publishing — see above.
- **Custom-import speaker probing is best-effort**. Bundles with multiple
  speakers but no metadata file default to one `speaker_0`. The wizard lets
  you correct this manually after import.
- **Storage-location toggle is currently a persistence-only switch**;
  installed voices stay where they were when their entry was first written
  to Room. Moving them between internal and external on toggle is a separate
  hardening item.
- **Three new sherpa-onnx families (ZipVoice, Pocket, Supertonic)** appear in
  the AAR but are NOT yet wired up in `SherpaTtsRuntime`. The catalog flags
  them `available: false` so they show up as "Coming Soon" in Browse.
- **Per-voice licenses vary** — the catalog's `license` field is best-effort
  scraped from upstream subpages, but check upstream HF / GitHub before
  redistributing any individual voice commercially.

## Codebase orientation

```
HayaiTTS/
├── app/
│   ├── libs/sherpa-onnx-1.13.2.aar      # the upstream AAR, LFS-tracked
│   ├── proguard-rules.pro               # R8 keep rules for JNI bindings
│   └── src/main/java/dev/ahmedmohamed/hayaitts/
│       ├── app/                         # Application + Koin module
│       ├── data/                        # Repository implementations
│       │   ├── catalog/                 # JSON catalog asset + remote refresh
│       │   ├── custom/                  # SAF-import bundle analyzer + installer
│       │   ├── db/                      # Room v2 (entities, DAOs, migrations)
│       │   ├── download/                # WorkManager + OkHttp + tar.bz2 extract
│       │   └── voices/                  # InstalledVoice repo with bundled stitch
│       ├── domain/                      # Plain Kotlin models + repo interfaces
│       ├── tts/                         # HayaiTtsService + SherpaTtsRuntime
│       │   ├── HayaiTtsService.kt       # extends TextToSpeechService
│       │   └── SherpaTtsRuntime.kt      # LRU of OfflineTts, per-family config
│       └── ui/                          # Compose screens, all M3 Expressive
│           ├── library/                 # Voices home (FAB → Browse, Import)
│           ├── browse/                  # Filtered catalog grid
│           ├── detail/                  # Per-voice details + preview Play
│           ├── custom/                  # SAF wizard
│           ├── settings/                # Engine settings (Downloads / Defaults / Storage / About)
│           ├── components/              # VoiceCard, TierChip, etc.
│           ├── nav/                     # NavHost route graph
│           └── theme/                   # MaterialExpressiveTheme + voice teal
├── catalog/v1/models.json               # Generated catalog
├── tools/catalog/build_catalog.py       # Generator script
└── .github/workflows/                   # CI: build + weekly catalog refresh
```

## Contact / contribution

Issues and PRs welcome at https://github.com/HayaiApp/HayaiTTS.

For changes that touch synthesis output, please attach a clip of the
"Listen to an example" output before and after your change — sherpa-onnx's
config surface is subtle and audible differences are easy to miss in code
review.
