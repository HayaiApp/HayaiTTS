# CLAUDE.md — HayaiTTS

Quick-reference for working in this repo. Detailed history and architecture
live in [`ONBOARDING.md`](ONBOARDING.md) and
[`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Build

```powershell
.\gradlew assembleDebug -Dorg.gradle.java.home="C:\Program Files\Android\Android Studio\jbr"
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

JDK 21 (JBR shipped with Android Studio). Gradle toolchain is pinned, so any
JDK 21 install works. Lint clean — keep it that way. `:app:test` runs the
Konsist architecture suite; treat failures as build errors, not warnings.

## Repo orientation

- `app/` — single Gradle module, all production code.
- `catalog/v1/models.json` — voice manifest, regenerated weekly by
  [`tools/catalog/build_catalog.py`](tools/catalog/build_catalog.py) via the
  [`catalog-refresh`](.github/workflows/catalog-refresh.yml) workflow.
- `docs/ARCHITECTURE.md` — layering, naming, threading, error handling,
  testing, forbidden patterns. **Read before adding a feature.**
- `ONBOARDING.md` — build phase history, signing, CI architecture,
  on-device smoke test.

## Architecture in one paragraph

UI → UseCase → Repository → Data source → sherpa-onnx runtime. Dependencies
point one way. `domain/` is pure Kotlin (no `android.*`, no `androidx.*`
except `androidx.annotation`); `data/` may import `domain` but not `ui`;
`ui/` may import both. Threading goes through the injected
[`DispatcherProvider`](app/src/main/java/dev/ahmedmohamed/hayaitts/core/dispatchers/DispatcherProvider.kt),
not `Dispatchers.*` directly. Repository returns flow through
[`Outcome<T>`](app/src/main/java/dev/ahmedmohamed/hayaitts/core/result/Outcome.kt)
rather than thrown exceptions. Konsist tests in
`app/src/test/java/.../core/konsist/` enforce these rules — break a rule and
CI goes red.

## Conventions

- No `Co-Authored-By: Claude` trailers on commits, no robot emoji in PR
  bodies. Just write the change.
- Strings ship in all 10 supported locales. CI diffs the key sets across
  `res/values-*/strings.xml`.
- Voice teal (`#0E7C86`) is the brand color. Don't override the M3 Expressive
  scheme; extend it.
- For UI changes attach a screenshot or short recording — Compose previews
  don't capture motion/haptics, which is most of where the UX lives.

## Forbidden

- Importing `kotlinx.coroutines.Dispatchers` outside `core/` (Workers and
  Receivers excepted — they are framework-constructed).
- Returning Room `*Entity` types from a `domain/` repo or referencing them
  from a `@Composable`.
- Throwing exceptions from a repository or use case — return `Outcome.Failure`.
- Adding a `build.gradle` version literal — every version lives in
  `gradle/libs.versions.toml`.
