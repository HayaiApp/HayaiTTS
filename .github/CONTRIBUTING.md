# Contributing to HayaiTTS

Thanks for considering a contribution! This is a small project but we
want every change to land cleanly.

## Setup

See [ONBOARDING.md](../ONBOARDING.md#build--install) for the build
prerequisites (JDK 21, Android SDK 36, Git LFS).

## Where things live

- **Inference + system TTS**: `app/src/main/java/dev/ahmedmohamed/hayaitts/tts/`
- **Compose UI**: `app/src/main/java/dev/ahmedmohamed/hayaitts/ui/`
- **Data + repositories**: `app/src/main/java/dev/ahmedmohamed/hayaitts/data/`
- **Domain models + repo interfaces**: `app/src/main/java/dev/ahmedmohamed/hayaitts/domain/`
- **Catalog**: `catalog/v1/models.json` (auto-generated weekly — don't
  hand-edit; tweak the generator at `tools/catalog/build_catalog.py`
  instead)
- **Workflows**: `.github/workflows/`

The full architecture tour is in [ONBOARDING.md](../ONBOARDING.md#codebase-orientation).

## Style + conventions

- **Material 3 Expressive only**. New UI uses `MaterialExpressiveTheme`,
  expressive components (`WavyProgressIndicator`,
  `LargeFlexibleTopAppBar`, `HorizontalFloatingToolbar`,
  `MultiChoiceSegmentedButtonRow`, etc.), and `MotionScheme.expressive()`
  springs. Don't fall back to plain `MaterialTheme` even when an API
  needs `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` —
  add the opt-in.
- **Comments document why, not what**. Well-named identifiers handle
  "what". Add comments for hidden constraints, subtle invariants,
  workarounds, or behavior that would surprise a reader.
- **One change per PR**. Refactors, features, and dependency bumps go
  in separate PRs even if they're individually small.
- **No `Co-Authored-By: Claude` lines in commits** — this repo's
  convention.

## Before opening a PR

1. `./gradlew assembleDebug` and `./gradlew assembleRelease` both pass.
2. The CI workflow (`build_check.yml`) does both on Blacksmith runners
   on every PR — but verify locally first if you can.
3. If you touched any UI, attach a screenshot or short screen recording
   in the PR description. Compose previews and unit tests don't capture
   motion / haptics / shape morphing.
4. If you touched synthesis, attach a short audio clip of the "Listen
   to an example" output before and after.

## Adding a new model family

If sherpa-onnx ships a JNI for a new TTS family:

1. Add the enum variant to
   `app/src/main/java/dev/ahmedmohamed/hayaitts/domain/model/ModelFamily.kt`.
2. Add a `buildXxxConfig(dir: File): OfflineTtsConfig` in
   `app/src/main/java/dev/ahmedmohamed/hayaitts/tts/SherpaTtsRuntime.kt`.
3. Add a branch to the required-files matrix in
   `app/src/main/java/dev/ahmedmohamed/hayaitts/data/download/VoiceDownloadWorker.kt`.
4. Add the family slug to `RUNTIME_SUPPORTED_FAMILIES` in
   `tools/catalog/build_catalog.py` and to `family_for_slug` if the
   upstream URL pattern needs it.
5. Add per-family seed color + glyph to
   `app/src/main/java/dev/ahmedmohamed/hayaitts/ui/components/FamilyIdentity.kt`.
6. Add display string to `res/values/strings.xml`.
7. Add a branch to the custom-import installer in
   `app/src/main/java/dev/ahmedmohamed/hayaitts/data/custom/CustomBundleInstaller.kt`.

The catalog refresh workflow will pick up the new family's voices on
its next weekly run.

## Reporting bugs

Use the [bug report template](https://github.com/HayaiApp/HayaiTTS/issues/new/choose).
Always include a device + Android version + voiceId; TTS bugs are
notoriously voice-specific.

## License

By contributing, you agree your work is released under
[GPL-3.0](../LICENSE) like the rest of the project.
