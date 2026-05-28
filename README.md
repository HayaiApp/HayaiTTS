<div align="center">

# HayaiTTS

**Free, offline neural voices for your Android phone.**

[![CI](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_check.yml/badge.svg)](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_check.yml)
[![Release](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_push.yml/badge.svg)](https://github.com/HayaiApp/HayaiTTS/actions/workflows/build_push.yml)
[![License: GPL v3](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Latest release](https://img.shields.io/github/v/release/HayaiApp/HayaiTTS?include_prereleases)](https://github.com/HayaiApp/HayaiTTS/releases)

</div>

> Replace your phone's robotic built-in voice with 188 natural-sounding
> neural voices, in 40+ languages. No internet, no accounts, no ads.

## What is HayaiTTS?

HayaiTTS is a free text-to-speech app for Android. It runs entirely on
your phone — once you install a voice, you can put your phone in
airplane mode and it still talks.

Any app that reads things aloud — your reading app, turn-by-turn
navigation, the accessibility screen reader — can use HayaiTTS instead
of the stock Google or Samsung voice.

## Screenshots

<!-- TODO: add phone screenshots: Browse, Voice Detail, Quick Switcher. -->

## Install (3 steps)

1. **Download the APK** from
   [the latest release](https://github.com/HayaiApp/HayaiTTS/releases).
   Tap the `.apk` to install — your phone will ask once for permission
   to install from this source.
2. **Pick a voice.** Open HayaiTTS, tap the **Browse** button, choose any
   voice. Piper "Amy" is a good starting point — it ships bundled
   with the app so it works instantly, no download required.
3. **Set HayaiTTS as your phone's voice.** Go to *Settings → System
   → Languages & input → Text-to-speech*, and pick **HayaiTTS** as the
   preferred engine. (The menu name varies by Android skin — look for
   "Preferred engine" or "Speech.")

Tap "Listen to an example" in your phone's TTS settings to verify it
worked. That's it — every app on the phone now speaks with neural
voices.

## How to use it

Once HayaiTTS is your phone's TTS engine, anything that speaks now
speaks with neural voices — no further setup. Try:

- **Your reading app** — most novel and ebook readers (including
  [Hayai](https://github.com/HayaiApp/hayai)) have a "read aloud"
  button.
- **Google Maps** — turn-by-turn directions.
- **Accessibility** — Android's screen reader (TalkBack) uses your
  default TTS engine.

Inside HayaiTTS itself you can:

- **Browse** 188 voices, grouped by language and family.
- **Preview** each voice with a play button before you download it.
- **Favorite** voices and pin them to the top of your library.
- **Set different defaults per language** — one voice for English, a
  different one for Japanese, another for French. HayaiTTS routes
  each request to the right voice automatically.
- **Import your own voice** — if you have a sherpa-onnx-compatible
  voice bundle, point HayaiTTS at it and it'll install.

## Voice catalog

188 neural voices across five model families. All are MIT- or
Apache-licensed upstream and free to use.

| Family | Voices | Best for |
|---|---|---|
| **Piper** | 174 | Most languages — small downloads (10–60 MB), fast |
| **Kitten** | 7 | English on lower-end phones — fastest |
| **Kokoro** | 3 | Highest quality, multilingual |
| **Matcha** | 3 | High-quality diffusion voices |
| **Supertonic** | 1 | Newest (2026) model |

The catalog updates weekly — new releases from the open-source TTS
community appear in the Browse list automatically.

## Privacy

- **No network during synthesis.** Once you download a voice, your
  phone speaks without ever phoning home.
- **No accounts. No telemetry. No analytics. No ads.**
- **Open source** (GPL-3.0) — every line of code is auditable.

The app only reaches the network for:

1. Loading the voice catalog (a single JSON file from this repo).
2. Downloading the model files you explicitly install.
3. Streaming the short preview clip when you tap a voice's play
   button.

## FAQ

**Is it really free?** Yes. GPL-3.0, no paid tier, no ads.

**Does it work offline?** Yes, after the initial voice download.

**Why isn't it on Google Play?** The Play Store doesn't allow apps
that bundle GPL code in the way HayaiTTS does. Sideload the APK from
[Releases](https://github.com/HayaiApp/HayaiTTS/releases).

**Will it drain my battery?** Synthesis is fast (sub-second on a
2020+ phone) and the engine sleeps between requests.

**Is there an iOS version?** No, and there can't be — Apple doesn't
let third-party apps replace the system TTS engine.

**Where do the voices come from?** They're trained by the open-source
TTS community — see
[sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx),
[Piper](https://github.com/rhasspy/piper), and the
[Kokoro project](https://huggingface.co/hexgrad/Kokoro-82M).

## Related apps

- **[Hayai](https://github.com/HayaiApp/hayai)** — the manga and
  novel reader that uses HayaiTTS for read-aloud.

---

## For developers

Building, architecture, voice catalog internals, release signing, and
the runtime smoke-test plan are documented separately:

- **[ONBOARDING.md](ONBOARDING.md)** — JDK/SDK requirements, build
  commands, release signing, architecture overview, phase-by-phase
  history.
- **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** — layering, naming,
  threading, error handling, testing, forbidden patterns.
- **[CLAUDE.md](CLAUDE.md)** — codebase quick-reference and conventions
  for AI-assisted contributions.
- **[catalog/v1/models.json](catalog/v1/models.json)** — the voice
  catalog. Regenerated weekly by
  [`tools/catalog/build_catalog.py`](tools/catalog/build_catalog.py).
- **[`render-samples.yml`](.github/workflows/render-samples.yml)** —
  per-(speaker, language) audition clips, rendered weekly into the
  [HayaiTTS-samples](https://github.com/HayaiApp/HayaiTTS-samples)
  releases.

Quick build:

```bash
git clone https://github.com/HayaiApp/HayaiTTS.git
cd HayaiTTS
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

PRs welcome. For UI changes, attach a screenshot or short recording —
Compose previews and unit tests don't capture motion/haptics, which
is where most of the UX value lives.

## License

GPL-3.0 — see [LICENSE](LICENSE). Individual voice licenses vary and
are captured per-entry in the catalog; most are MIT or Apache-2.0
upstream. Check the upstream source on Hugging Face / GitHub before
redistributing any individual voice commercially.
