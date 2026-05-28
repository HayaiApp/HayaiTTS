# HayaiTTS — onboarding

For anyone (including future you) who needs to understand the repo's
history, build it, ship it, and extend it without re-reading every
commit message.

## Build phase history

| # | Anchor commit | What landed |
|---|---|---|
| 1 | (in Hayai repo as `hayai-phase1-tts-ux.patch`) | Hayai-side three-axis engine/language/voice TTS settings, "Test voice" button, HayaiTTS handoff card. Captured as a patch — apply with `git apply hayai-phase1-tts-ux.patch` on a clean Hayai working tree. |
| 2 | `5010e3b` + `04c0f00` | Repo bootstrap: gradle, M3 Expressive shell (material3 1.5.0-alpha19), branding fork (Hayai SVGs + speaker-wave glyph + voice teal `#0E7C86`), 440 Hz `TextToSpeechService` stub registering as a system engine. |
| 3 | `58840b7` | Real synthesis: sherpa-onnx wired in, bundled Piper Amy `en_US-amy-low` voice, multi-buffer 16-bit PCM streaming through `SynthesisCallback`. |
| 4 | `51e065d` + `2bda965` | Catalog + downloader data layer (Room v1, OkHttp + WorkManager + Apache Commons Compress) and Browse / Voice Detail UI with navigation-compose. |
| 5 | `2449bd3` | Per-family config builders for Kokoro / Kitten / Matcha / VITS, Matcha vocoder sidecar download, "Coming Soon" badge for unrunable families. |
| 6 | `44d78df` | Custom-import wizard (SAF → Analyzing → Confirm → Importing → Done), Room v2 migration adding `effectiveFamily`. |
| 7 | `494f166` | Polish: ABI splits, R8 minify + resource shrink, settings UI, tier auto-recommendation, cleanup-on-failure, upstream `sherpa-onnx-1.13.2.aar` swap unlocking real Kokoro + Kitten JNI. |
| H | `181b335`, `5f1ecde`, `5da76c8`, `808195c`, `4d741c9`, `f16660f`, `3c70683` | Hardening: catalog generator + sha256 enforcement, release-signing template, CI workflows, real storage-location move, custom-import speaker probing, runtime support for ZipVoice / Pocket / Supertonic, README + this file. |
| U | `c7c15da` | UI/UX overhaul: global `VoiceQuickSwitcher` bottom sheet, Library featured/favorites/reorder, family-tinted gradients + shape-morphing badges in Browse, immersive Voice Detail hero with live 32-bin waveform, `MultiChoiceSegmentedButtonRow`, `LargeFlexibleTopAppBar`, `HorizontalFloatingToolbar`, full `MotionScheme.expressive()` motion. |
| R | `a33297e` + `24ae248` + `063e27a` | Release infra: real RSA-4096 keystore with creds in encrypted GitHub secrets, signed via direct `apksigner` (not the flavor-coupled third-party action), same-repo nightlies tagged `r-NN`, weekly automated catalog refresh that commits directly to main. |

## Build & install

### One-time prerequisites

- **JDK 21** — the JBR shipped with Android Studio works
  (`C:\Program Files\Android\Android Studio\jbr` on the dev machine);
  Temurin 21 from anywhere also works.
- **Android SDK** with platform 36 and build-tools 36.0.0.
- **Git LFS** — bundled `.onnx` weights + the vendored `sherpa-onnx-1.13.2.aar`
  are LFS-tracked.
- **Python 3.12+** with `requests` (only if you intend to regenerate
  the catalog locally — CI does this for you weekly).

### Debug builds

```powershell
.\gradlew assembleDebug -Dorg.gradle.java.home="C:\Program Files\Android\Android Studio\jbr"
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release signing

CI signs every release with our production keystore. To produce a
locally-signed release APK too, drop the keystore + a `signing.properties`
file at the repo root:

```bash
# Generate a fresh keystore (only if you're starting over — CI already has one).
keytool -genkeypair -v \
    -keystore hayaitts-release.jks \
    -keyalg RSA -keysize 4096 -validity 36500 \
    -alias hayaitts -storetype JKS

# Copy the template and fill in real values.
cp signing.properties.template signing.properties
```

Then `assembleRelease` picks the credentials up automatically:

```powershell
.\gradlew assembleRelease -Dorg.gradle.java.home="C:\Program Files\Android\Android Studio\jbr"
```

Output APKs land at `app/build/outputs/apk/release/`. `.gitignore`
excludes `signing.properties`, `*.jks`, `*.keystore` — never commit
those.

If `signing.properties` is absent the release variant emits **unsigned**
APKs (named `app-*-release-unsigned.apk`). That's what CI does — see
[`.github/workflows/build_push.yml`](.github/workflows/build_push.yml)
for the `apksigner` step that converts those to signed APKs using the
secrets.

### CI architecture

| Workflow | Runner | Trigger | Job |
|---|---|---|---|
| `build_check.yml` | `blacksmith-8vcpu-ubuntu-2404` | PR | `assembleRelease` + `assembleDebug`, upload arm64-v8a + universal APKs as artifacts |
| `build_push.yml` | `blacksmith-8vcpu-ubuntu-2404` | push to `main`, manual dispatch | Build, sign with `apksigner`, create stable / beta / nightly Release |
| `catalog-refresh.yml` | `ubuntu-latest` | weekly cron, manual dispatch | Run `tools/catalog/build_catalog.py`, commit new catalog to `main` |

Blacksmith carries the CPU-bound builds (Kotlin compile + R8 dominate
those minutes). The catalog refresh is bandwidth-bound (~10 GB streamed
across ~190 bundles) so it stays on GitHub-hosted free runners.

### Release tag scheme

| Stream | Tag | Draft | Prerelease |
|---|---|---|---|
| Stable | `v1.0.0` | yes (auto-drafted, human approves) | false |
| Beta | `v1.0.0-b1` | yes | true |
| Nightly | `r17` | no (auto-published) | true |

Nightlies live in the **same** `HayaiApp/HayaiTTS` repo (not a separate
`-nightly` repo). The `r` prefix is the discriminator the in-app
auto-updater filters on.

## Runtime smoke test (on a real device)

CI verifies the build itself but cannot exercise the TTS service
end-to-end — no Android emulator can confirm "does it actually speak".
Run this once per release on real hardware:

```bash
# 1. Sideload the universal APK.
adb install -r hayaitts-v1.0.0-b1.apk

# 2. Open the system TTS settings page directly.
adb shell am start -a android.settings.TTS_SETTINGS

# 3. In the engine picker, choose HayaiTTS. Tap "Listen to an example".
#    You should hear Amy speak "The quick brown fox..." in clean en-US.
#    If you hear silence or a 440 Hz tone, check logcat:
adb logcat -s HayaiTtsService SherpaTtsRuntime

# 4. Open HayaiTTS. Tap the FAB → Browse. Install the smallest non-bundled
#    voice you can find (e.g. `vits-piper-en_GB-alan-low`, ~63 MB).
#    Confirm the download wavy progress bar advances smoothly,
#    "Extracting…" indeterminate spinner takes over for ~10 s,
#    the voice appears in Library with a "Set default" toggle.

# 5. Tap the new voice → Voice Detail. Hit Play on the preview.
#    The 32-bin waveform should pulse with the audio level.

# 6. Custom-import: download any other Piper voice's `.tar.bz2` to the
#    device, hit Library → Import, pick the file. Wizard should:
#       - detect the family from the file layout
#       - show real speakers if the bundle has speakers.txt or similar
#       - install with progress, surface a "Custom" chip on the card

# 7. From the system TTS settings page, switch the preferred engine to
#    HayaiTTS, then open a TTS-consuming app (e.g. Hayai's novel reader)
#    and verify your installed voices are speaking the text.
```

If any step diverges, capture the logcat and open an issue — UI/UX
regressions in particular are easy to miss in compile-only CI.

## Codebase orientation

```
HayaiTTS/
├── catalog/v1/models.json               # 600+ voices, auto-refreshed weekly
├── tools/catalog/build_catalog.py       # Scrapes upstream + enriches + sha256
├── tools/catalog/sources.py             # HF / GitHub model-card metadata
├── tools/catalog/overlays/voices.yaml   # Hand-curated flagship descriptions
├── app/
│   ├── libs/sherpa-onnx-1.13.2.aar      # Inference runtime, LFS-tracked
│   ├── proguard-rules.pro               # R8 keep rules for JNI bindings
│   └── src/main/
│       ├── assets/voices/en_US-amy-low/ # Bundled Piper Amy
│       ├── res/                         # Strings, themes, launcher icons
│       └── java/dev/ahmedmohamed/hayaitts/
│           ├── app/                     # Application + Koin module
│           ├── data/                    # Repository implementations
│           │   ├── catalog/             # Bundled-asset + remote refresh
│           │   ├── custom/              # SAF-import analyzer + installer
│           │   ├── db/                  # Room v2: entities + DAOs + migrations
│           │   ├── defaults/            # Per-locale default voice
│           │   ├── download/            # WorkManager + OkHttp + tar.bz2
│           │   ├── preview/             # AudioTrack player + amplitude flow
│           │   ├── settings/            # DataStore-backed prefs
│           │   ├── storage/             # StorageMigrator (internal ⇄ external)
│           │   └── voices/              # InstalledVoice repo + bundled stitch
│           ├── domain/                  # Plain Kotlin models + repo interfaces
│           ├── tts/                     # HayaiTtsService + SherpaTtsRuntime
│           │   ├── HayaiTtsService.kt   # extends TextToSpeechService
│           │   └── SherpaTtsRuntime.kt  # LRU of OfflineTts; per-family config
│           └── ui/                      # All Compose screens, M3 Expressive
│               ├── library/             # Voices home + featured + reorder
│               ├── browse/              # Filtered catalog with segmented tier
│               ├── detail/              # Per-voice hero + live waveform
│               ├── custom/              # SAF import wizard
│               ├── settings/            # Engine settings (Downloads / Defaults / Storage / About)
│               ├── quickswitch/         # Global voice-switcher bottom sheet
│               ├── components/          # VoiceCard, FamilyIdentity, chips, progress
│               ├── nav/                 # NavHost route graph
│               └── theme/               # MaterialExpressiveTheme + voice teal
└── .github/
    ├── branding/                        # Master SVGs + build_branding.py
    ├── runner-files/                    # CI gradle.properties
    └── workflows/                       # build_check + build_push + catalog-refresh
```

## Known limits

- **Bundled voice can't be moved**. The Piper Amy voice mirrored from
  `assets/` is regenerable, so the storage-location migrator skips it.
  Practical impact: zero — the mirror is ~78 MB on internal storage
  even when the rest of your voices live on the SD card.
- **ZipVoice + Pocket now ship in the catalog** alongside Piper, VITS,
  Kokoro, Kitten, Matcha, and Supertonic. As of v2.5 the generator
  enumerates the *full* sherpa-onnx `tts-models` release via the paginated
  GitHub assets API (the old tag endpoint truncated the list), so bundles
  the doc index missed are no longer dropped. The Meta MMS / Coqui / MeloTTS
  VITS voices that were already present now carry correct per-voice language
  (BCP-47) and tier metadata instead of a blanket English / mid guess.
- **Custom-import speaker probing is best-effort**. Bundles without a
  `speakers.txt` / `voices.txt` / `speaker_id_map` JSON default to one
  `speaker_0`. The wizard lets you correct this manually after import.
- **Per-voice licenses vary**. As of v2.5 the catalog's `license`,
  `dataset`, `author`, and `baseModel` fields are scraped per-voice from
  Hugging Face model cards + GitHub repos (with family defaults as a
  fallback). Piper voices report MIT weights but may carry a restrictive
  *dataset* license surfaced as a `dataset-license:` tag — still check
  upstream before redistributing any individual voice commercially.

## Catalog regeneration (rare, usually CI's job)

If you ever need to regenerate the catalog locally (e.g. while iterating
on the generator script):

```bash
pip install -r tools/catalog/requirements.txt
python tools/catalog/build_catalog.py --output catalog/v1/models.json
```

The first run streams ~5–10 GB across ~190 bundles. Subsequent runs
with `--cache-dir .catalog_cache` skip the hash step for cached
entries.

In practice you almost never want to run this locally — the
[`catalog-refresh`](.github/workflows/catalog-refresh.yml) workflow
does it weekly on a free GitHub-hosted runner and pushes the result
straight to `main`. To force an immediate run:

```bash
gh workflow run catalog-refresh.yml --repo HayaiApp/HayaiTTS
```

## Contact / contribution

Issues and PRs welcome at https://github.com/HayaiApp/HayaiTTS.

For PRs that touch synthesis output, please attach a clip of the
"Listen to an example" output before and after your change —
sherpa-onnx's config surface is subtle and audible differences are easy
to miss in code review.
