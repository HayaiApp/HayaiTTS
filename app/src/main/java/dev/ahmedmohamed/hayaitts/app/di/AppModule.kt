package dev.ahmedmohamed.hayaitts.app.di

import dev.ahmedmohamed.hayaitts.core.dispatchers.DefaultDispatcherProvider
import dev.ahmedmohamed.hayaitts.core.dispatchers.DispatcherProvider
import dev.ahmedmohamed.hayaitts.data.catalog.CatalogRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.custom.CustomBundleAnalyzer
import dev.ahmedmohamed.hayaitts.data.custom.CustomBundleInstaller
import dev.ahmedmohamed.hayaitts.data.db.HayaiTtsDatabase
import dev.ahmedmohamed.hayaitts.data.defaults.DefaultsRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.download.DownloadRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.onboarding.OnboardingPreferences
import dev.ahmedmohamed.hayaitts.data.playground.SampleHistoryRepository
import dev.ahmedmohamed.hayaitts.data.playground.VoiceTuningRepository
import dev.ahmedmohamed.hayaitts.data.preview.VoicePreviewPlayer
import dev.ahmedmohamed.hayaitts.data.settings.SettingsRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.storage.StorageMigrator
import dev.ahmedmohamed.hayaitts.data.update.UpdateChecker
import dev.ahmedmohamed.hayaitts.data.update.UpdateInstaller
import dev.ahmedmohamed.hayaitts.data.voices.VoiceRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.telemetry.SynthesisTelemetryRepository
import dev.ahmedmohamed.hayaitts.data.tts.SherpaSynthesisGateway
import dev.ahmedmohamed.hayaitts.ui.activity.ActivityViewModel
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import dev.ahmedmohamed.hayaitts.domain.usecase.InstallVoiceUseCase
import dev.ahmedmohamed.hayaitts.domain.usecase.RecommendTierUseCase
import dev.ahmedmohamed.hayaitts.domain.usecase.RefreshCatalogUseCase
import dev.ahmedmohamed.hayaitts.domain.usecase.SynthesisGateway
import dev.ahmedmohamed.hayaitts.domain.usecase.SynthesizeUseCase
import dev.ahmedmohamed.hayaitts.ui.browse.BrowseViewModel
import dev.ahmedmohamed.hayaitts.ui.custom.CustomImportViewModel
import dev.ahmedmohamed.hayaitts.ui.detail.VoiceDetailViewModel
import dev.ahmedmohamed.hayaitts.ui.downloads.DownloadsHistory
import dev.ahmedmohamed.hayaitts.ui.downloads.DownloadsViewModel
import dev.ahmedmohamed.hayaitts.ui.library.LibraryViewModel
import dev.ahmedmohamed.hayaitts.ui.library.preferences.LibraryUiPreferences
import dev.ahmedmohamed.hayaitts.ui.playground.PlaygroundViewModel
import dev.ahmedmohamed.hayaitts.ui.quickswitch.QuickSwitchViewModel
import dev.ahmedmohamed.hayaitts.ui.settings.SettingsViewModel
import dev.ahmedmohamed.hayaitts.ui.update.UpdateViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/**
 * Wired up by HayaiTtsApplication.startKoin.
 */
val appModule = module {

    // Centralized dispatcher provider. Production code must depend on
    // [DispatcherProvider] rather than `Dispatchers.*` directly so the Konsist
    // architecture suite can enforce the rule and tests can substitute a
    // TestDispatcher.
    single<DispatcherProvider> { DefaultDispatcherProvider() }

    // Long-lived application scope used by repositories that need to launch
    // their own coroutines (catalog refresh, download state writes, etc.).
    // SupervisorJob so one failure does not cancel the rest.
    single(named("appScope")) {
        CoroutineScope(SupervisorJob() + get<DispatcherProvider>().default)
    }

    // OkHttp shared by the catalog refresh + voice downloader.
    single {
        OkHttpClient.Builder()
            .callTimeout(10, TimeUnit.MINUTES)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Database + DAOs. Each DAO is a single Koin definition so the worker can
    // resolve them without going through the database aggregate.
    single { HayaiTtsDatabase.build(androidContext()) }
    single { get<HayaiTtsDatabase>().installedVoiceDao() }
    single { get<HayaiTtsDatabase>().downloadStateDao() }
    single { get<HayaiTtsDatabase>().defaultVoiceDao() }
    single { get<HayaiTtsDatabase>().playgroundSampleDao() }
    single { get<HayaiTtsDatabase>().voiceProfileDao() }
    single { get<HayaiTtsDatabase>().pronunciationDao() }
    single { get<HayaiTtsDatabase>().appRouteDao() }

    // Phase 7c: SSML preprocessor singleton — purely functional, no Android deps.
    single { dev.ahmedmohamed.hayaitts.data.ssml.SsmlPreprocessor() }

    // Repositories. Bind both the interface and the impl class so callers can
    // pick either (the TTS service grabs the impl for the snapshot helper).
    single<SettingsRepository> { SettingsRepositoryImpl(androidContext()) }
    single<VoiceRepositoryImpl> { VoiceRepositoryImpl(androidContext(), get()) }
    single<VoiceRepository> { get<VoiceRepositoryImpl>() }
    single<CatalogRepository> {
        CatalogRepositoryImpl(
            context = androidContext(),
            okHttp = get(),
            externalScope = get(named("appScope")),
            dispatchers = get(),
        )
    }
    single<DownloadRepository> {
        DownloadRepositoryImpl(
            context = androidContext(),
            downloadStateDao = get(),
            settings = get(),
            appScope = get(named("appScope")),
            dispatchers = get(),
        )
    }
    single<DefaultsRepository> { DefaultsRepositoryImpl(get()) }

    // Phase 4b: short-lived AudioTrack helper for Voice Detail previews.
    single { VoicePreviewPlayer(androidContext(), get()) }

    // v2: streaming MediaPlayer for the hosted sample audio surfaced on
    // Voice Detail before the user downloads the model.
    single { dev.ahmedmohamed.hayaitts.data.preview.SampleAudioPlayer(get()) }

    // P5 Playground: per-voice DataStore-backed tuning prefs + Room-backed
    // sample-history repo. Both are app-singletons because the playground
    // screen and its sibling Voice Detail observe the same flows.
    single { VoiceTuningRepository(androidContext()) }
    single { SampleHistoryRepository(androidContext(), get()) }

    // Hardening pass: resolves voice install paths and moves voices between
    // internal storage and a mounted SD card when the user flips the setting.
    single {
        StorageMigrator(
            context = androidContext(),
            installedVoiceDao = get(),
            settings = get(),
            dispatchers = get(),
        )
    }

    // Phase 6: custom voice import services.
    single { CustomBundleAnalyzer(androidContext()) }
    single { CustomBundleInstaller(androidContext(), get<VoiceRepository>()) }

    // UI-only DataStore for the Library reorder + favorites state. Kept under
    // ui/ so the data + domain layers stay independent of presentation state.
    single { LibraryUiPreferences(androidContext()) }

    // P2: DataStore-backed completion history for the Downloads Manager.
    single { DownloadsHistory(androidContext()) }

    // First-launch onboarding flag, separate DataStore so a future "reset
    // onboarding" action can wipe it without touching engine settings.
    single { OnboardingPreferences(androidContext()) }

    // Auto-updater. Uses the shared OkHttp + SettingsRepository so the channel
    // preference + 6h cooldown live in the existing hayai_settings DataStore.
    single { UpdateChecker(okHttp = get(), settings = get(), dispatchers = get()) }
    single { UpdateInstaller(context = androidContext(), okHttp = get(), dispatchers = get()) }

    // Phase 11b: SynthesisGateway adapter + the four use-cases. Domain
    // consumers (use cases) depend on the gateway interface, never on the
    // sherpa runtime directly.
    single { SynthesisTelemetryRepository() }
    single<SynthesisGateway> { SherpaSynthesisGateway(androidContext(), get()) }
    factory { InstallVoiceUseCase(catalog = get(), downloads = get()) }
    factory { RefreshCatalogUseCase(catalog = get()) }
    factory { RecommendTierUseCase() }
    factory { SynthesizeUseCase(gateway = get()) }

    viewModel { LibraryViewModel(get(), get(), get(), get(), get()) }
    viewModel { BrowseViewModel(androidContext(), get(), get(), get(), get()) }
    viewModel { (voiceId: String) ->
        VoiceDetailViewModel(
            voiceId = voiceId,
            catalogRepository = get(),
            voiceRepository = get(),
            downloadRepository = get(),
            defaultsRepository = get(),
            previewPlayer = get(),
        )
    }
    viewModel { (voiceId: String) ->
        PlaygroundViewModel(
            voiceId = voiceId,
            catalogRepository = get(),
            voiceRepository = get(),
            tuningRepo = get(),
            historyRepo = get(),
            previewPlayer = get(),
        )
    }
    viewModel { QuickSwitchViewModel(get(), get()) }
    viewModel { (encodedUri: String) ->
        CustomImportViewModel(
            encodedUri = encodedUri,
            analyzer = get(),
            installer = get(),
            dispatchers = get(),
        )
    }
    viewModel {
        SettingsViewModel(
            context = androidContext(),
            settings = get(),
            voices = get(),
            defaults = get(),
            migrator = get(),
            dispatchers = get(),
            catalog = get(),
        )
    }
    viewModel {
        UpdateViewModel(
            settings = get(),
            checker = get(),
            installer = get(),
        )
    }
    viewModel {
        DownloadsViewModel(
            downloadRepository = get(),
            catalogRepository = get(),
            voiceRepository = get(),
            history = get(),
        )
    }
    viewModel {
        ActivityViewModel(
            downloads = get(),
            voices = get(),
            catalog = get(),
            telemetry = get(),
        )
    }
}
