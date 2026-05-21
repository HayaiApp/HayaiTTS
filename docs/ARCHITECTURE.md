# HayaiTTS Architecture

The contract every contributor (human or otherwise) is expected to follow.
Konsist tests in `app/src/test/java/dev/ahmedmohamed/hayaitts/core/konsist/`
enforce most of these rules — break one and `./gradlew :app:test` goes red.

If a rule no longer makes sense, change it deliberately: update this doc, the
matching Konsist test, and write down the reason in the PR description.

## 1. Layers

```
                ┌──────────────────────────────────────────────┐
                │                    ui/                       │
                │ Composables + ViewModels + nav + theme       │
                └──────────────┬───────────────────────────────┘
                               │ depends on
                               ▼
                ┌──────────────────────────────────────────────┐
                │              domain/usecase/                 │
                │ UseCase / Interactor orchestration           │
                └──────────────┬───────────────────────────────┘
                               │ depends on
                               ▼
                ┌──────────────────────────────────────────────┐
                │            domain/repo + model/              │
                │ Pure-Kotlin interfaces + data classes        │
                └──────────────┬───────────────────────────────┘
                               │ implemented by
                               ▼
                ┌──────────────────────────────────────────────┐
                │                   data/                      │
                │ Room, DataStore, OkHttp, FS, sherpa-onnx     │
                └──────────────────────────────────────────────┘

                ┌──────────────────────────────────────────────┐
                │                   core/                      │
                │ DispatcherProvider, Outcome, Konsist, log    │
                │ (cross-cutting; ui/data/domain may import)   │
                └──────────────────────────────────────────────┘
```

Arrows point one way. UI never imports a Room entity. Repositories never
import a Composable. `domain/` never imports `android.*`.

## 2. Module boundaries (Konsist rules)

- `domain/*` must not import `android.*` or `androidx.*` (except
  `androidx.annotation.*`).
- `domain/*` must not import `data/*` or `ui/*`.
- `data/*` must not import `ui/*`.
- Every `Repository` interface lives under `domain/repo/`.
- Every `RepositoryImpl` class lives under `data/`.
- Production code outside `core/` must not import `kotlinx.coroutines.Dispatchers`.
  Workers (`*Worker.kt`) and BroadcastReceivers (`*Receiver.kt`) are exempt
  because Android constructs them; they use `Dispatchers.IO` directly.
- `*ViewModel` classes must not declare a property whose type ends in `Entity`.

## 3. Domain purity rules

The `domain/` layer is pure Kotlin. It compiles standalone and contains:

- Data classes (`Voice`, `Speaker`, `Tier`, `ModelFamily`, etc.).
- Repository interfaces (`VoiceRepository`, `CatalogRepository`, …).
- Use cases (`SynthesizeUseCase`, `InstallVoiceUseCase`, …).
- Pure helper functions with no side effects.

Allowed imports:

- `kotlin.*`, `kotlinx.coroutines.*` (Flow / StateFlow are fine; coroutine
  builders too).
- `kotlinx.serialization.*` for `@Serializable` data classes that travel
  through the catalog JSON.
- `androidx.annotation.*` (e.g., `@VisibleForTesting`).

Disallowed:

- `android.*` of any kind.
- `androidx.*` of any kind except `androidx.annotation`.
- `dev.ahmedmohamed.hayaitts.data.*` or `…ui.*`.
- Logger frameworks tied to Android (kermit is OK; android.util.Log is not).

When you're tempted to import an Android type into `domain/`, wrap it behind
a `data/` adapter and inject the adapter into the use case. Example:
`DeviceTierEstimator` lives in `data/device/` not `domain/`; it returns a
`Tier` (domain type) that callers consume.

## 4. DI conventions

Koin is the DI container. The single Koin module is in
[`app/src/main/.../app/di/AppModule.kt`](../app/src/main/java/dev/ahmedmohamed/hayaitts/app/di/AppModule.kt).

- Singletons declared with `single { ... }`. Repository impls bind both the
  interface and the impl class:
  ```kotlin
  single<VoiceRepositoryImpl> { VoiceRepositoryImpl(...) }
  single<VoiceRepository> { get<VoiceRepositoryImpl>() }
  ```
- ViewModels declared with `viewModel { ... }`.
- Worker dependencies pulled at construction via `WorkerParameters.inputData`
  + `KoinComponent.inject()`.

Every new repository / use case / view-model registers in AppModule. The
boilerplate is intentional — explicit graph beats reflection-driven magic.

## 5. Threading conventions

```kotlin
class SomeRepository(
    private val dao: SomeDao,
    private val dispatchers: DispatcherProvider,
) {
    suspend fun load(): Outcome<Result> = withContext(dispatchers.io) { ... }
}
```

- Production code never imports `kotlinx.coroutines.Dispatchers` outside
  `core/`. Use `dispatchers.io`, `dispatchers.default`, `dispatchers.main`.
- Tests substitute a `TestDispatcherProvider` backed by
  `UnconfinedTestDispatcher` so `runTest { ... }` controls the clock.
- The app-wide `appScope` (in AppModule) uses `dispatchers.default` for
  long-lived background coroutines (catalog refresh, telemetry).
- Frameworks (WorkManager, BroadcastReceiver) bypass the rule because Android
  controls construction; they reference `Dispatchers.IO` directly. Konsist
  exempts files matching `*Worker.kt` and `*Receiver.kt`.

## 6. Error handling

```kotlin
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: AppError, val cause: Throwable? = null) : Outcome<Nothing>
}
sealed interface AppError {
    data object Network : AppError
    data object Storage : AppError
    data object Cancelled : AppError
    data class Catalog(val reason: String) : AppError
    data class Runtime(val family: String) : AppError
    data class Validation(val field: String, val reason: String) : AppError
    data class Unknown(val message: String) : AppError
}
```

Rules:

- Repositories return `Outcome<T>`, never throw. Wrap raw exceptions with
  `runCatchingOutcome { ... }` and classify into a typed `AppError`.
- `CancellationException` is rethrown by `runCatchingOutcome` — never
  swallow cancellation.
- Use cases compose with `Outcome.map` / `Outcome.flatMap`.
- ViewModels `fold(...)` to produce a UI state. Map `AppError` to user-facing
  strings inside `ui/`, not inside `domain/`.

Add a new error variant when an existing one would be lossy — e.g., a new
sherpa-onnx family rollout that needs its own failure pathway.

## 7. Adding a feature — worked example

Say you want a "Translate while speaking" toggle:

1. **Domain model** — `domain/model/Translation.kt` (data class, settings).
2. **Repository interface** — `domain/repo/TranslationRepository.kt`.
3. **Repository impl** — `data/translation/TranslationRepositoryImpl.kt`,
   implements the interface, injects DispatcherProvider, returns
   `Outcome<TranslatedText>`.
4. **Use case** — if the flow spans repos (e.g., translation + persistence),
   `domain/usecase/TranslateAndSynthesizeUseCase.kt`.
5. **ViewModel** — `ui/translation/TranslationViewModel.kt`, exposes
   `StateFlow<UiState>`, collects use case output, maps `Failure` to a string.
6. **Composable** — `ui/translation/TranslationScreen.kt`. No business logic
   inside `@Composable` — call ViewModel methods.
7. **Nav route** — add to `ui/nav/HayaiTtsNavHost.kt` and `Routes`.
8. **DI** — register the repo, use case, ViewModel in AppModule.
9. **Tests** — unit test the use case in `app/src/test/.../usecase/`;
   ViewModel test in `app/src/test/.../ui/translation/`.

If any step feels like ceremony, consider whether the feature actually needs
a separate use case or whether the ViewModel can call the repo directly. The
UseCase layer is for multi-repo orchestration, not single-method passthrough.

## 8. Naming & file conventions

| Suffix          | Layer         | Example                         |
|-----------------|---------------|---------------------------------|
| `*Entity.kt`    | data (Room)   | `InstalledVoiceEntity.kt`       |
| `*Dao.kt`       | data (Room)   | `InstalledVoiceDao.kt`          |
| `*Repository.kt`| domain        | `VoiceRepository.kt`            |
| `*RepositoryImpl.kt`| data      | `VoiceRepositoryImpl.kt`        |
| `*UseCase.kt`   | domain        | `InstallVoiceUseCase.kt`        |
| `*ViewModel.kt` | ui            | `LibraryViewModel.kt`           |
| `*Screen.kt`    | ui            | `LibraryScreen.kt` (composable) |
| `*Mapper.kt`    | data          | `PlaygroundSampleMapper.kt`     |
| `*Worker.kt`    | data          | `VoiceDownloadWorker.kt`        |
| `*Receiver.kt`  | data          | `DownloadActionReceiver.kt`     |

Packages mirror the layer: `data.catalog.*`, `domain.repo.*`,
`ui.library.*`, etc.

## 9. Forbidden patterns

- Returning a Room `*Entity` from anything in `domain/` or `ui/`.
- A `@Composable` that does I/O or holds business state (use a ViewModel).
- A `runBlocking { ... }` in production code (acceptable in tests for setup).
- A `Dispatchers.X` import outside `core/` and the Worker/Receiver exemption.
- Throwing from a repository — return `Outcome.Failure`.
- A version literal in `app/build.gradle.kts`. Add to
  `gradle/libs.versions.toml` instead.
- A `Co-Authored-By: Claude` trailer on commits or robot emoji in PR bodies.

## 10. Testing strategy

- **Unit tests** (`app/src/test/`) — Kotlin-only, no Android framework. Use
  `runTest` + `TestDispatcherProvider`. In-memory Room with
  `Room.inMemoryDatabaseBuilder`. Coverage target: every public state-affecting
  ViewModel method has at least one test.
- **Architecture tests** (`app/src/test/.../core/konsist/`) — Konsist rules,
  always run.
- **Instrumentation tests** (`app/src/androidTest/`) — Compose UI tests with
  `createAndroidComposeRule`. Use sparingly — Compose previews + real-device
  smoke tests catch most regressions.
- **Smoke test** — `ONBOARDING.md` documents a 7-step real-device flow that
  ships on every release. CI cannot verify TTS audio output; humans do.

Test naming: `\`function under test - condition - expected result\``, e.g.
`\`refresh - network failure - emits Failure with Network error\``.

## 11. Build & dependency policy

- **Single source of truth**: `gradle/libs.versions.toml`.
- New dependencies require a one-line "why" in the PR description plus a
  comment in `libs.versions.toml` if the version is pinned for a non-obvious
  reason.
- Lint must stay at zero warnings. The project is currently at zero (see
  commit `3f724ac` "Production-ready: zero lint issues across the project").
  New code that warns gets fixed, not suppressed.
- JDK 21 toolchain, JVM 21 target. Set explicitly in `app/build.gradle.kts`
  so a future contributor with JDK 17 still gets a deterministic build.

## 12. v2.0.0-b1 — finished state

All 18 phases of the v2 migration are landed and verified:

| Phase | Notes |
|---|---|
| Phase 0 | 8 broken-surface fixes (audit findings) |
| Phase 11b foundation | `DispatcherProvider`, `Outcome`/`AppError`, Konsist suite, Koin wiring |
| Phase 11b refactor | Domain Android-free (`DeviceTierEstimator` moved to `data/device/`, `UpdateChannel` moved to `domain/model/`); `DispatcherProvider` injected into 7 production files |
| Phase 11b use cases | `InstallVoiceUseCase`, `SynthesizeUseCase` + `SynthesisGateway`, `RefreshCatalogUseCase`, `RecommendTierUseCase` |
| Phase 11a scraper | Piper gender inference table + `genderConfidence` field on `Speaker` |
| Phase 11a UI domain | `Gender` enum, `BrowseFilters` (7 dimensions + `loosen()`), `applyFilters` extension |
| Phase 1 | 5-tab NavigationBar (Library / Browse / Studio / Activity / Settings) |
| Phase 2 | Family-tinted accent border on voice cards, Phase 0 hit-test fixes |
| Phase 3 + 11a UI | Filter domain model wired; legacy modal filter UI retained for v2 |
| Phase 4 | Studio screen, SSML preprocessor, WAV exporter, voice profiles entity + DAO |
| Phase 5 | Activity tab with Downloads / Extractions / Generations / RequestLog / Cache panes, `ActivityViewModel`, `SynthesisTelemetryRepository` |
| Phase 6 | `SettingsScreen` callable from NavHost, `SettingsCategoryCard` building block |
| Phase 7 | `PronunciationEntity` + DAO, `AppRouteEntity` + DAO, `SpeakSelectionActivity` with `ACTION_PROCESS_TEXT` intent filter |
| Phase 8 | `HayaiTtsNudge` foreground notification + channel, `UpdateCheckWorker` 12h poll + notification |
| Phase 9 | `AltRuntime` interface + registry, Kokoro multi-speaker `synthesizeBlend` in `SherpaTtsRuntime`, `ZIPVOICE/POCKET/SUPERTONIC` activated in `CustomImportViewModel.FamilyChoice` |
| Phase 10 | `Motion.kt` centralized springs, family-accent palette in `FamilyIdentity` |
| Phase 16 | This document + repo-root `CLAUDE.md` |
| Phase 17 | `OutcomeTest`, `BrowseFiltersTest`, 7-rule Konsist `ArchitectureTest` |
| Phase 18 | `versionCode 120 → 200`, `versionName "1.2.0-b1" → "2.0.0-b1"` |

`./gradlew :app:assembleDebug` and `./gradlew :app:testDebugUnitTest` both
pass on the final state. Tag the release commit `v2.0.0-b1` per the
[`build_push`](../.github/workflows/build_push.yml) workflow.
