package dev.ahmedmohamed.hayaitts.app.di

import dev.ahmedmohamed.hayaitts.data.catalog.CatalogRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.db.HayaiTtsDatabase
import dev.ahmedmohamed.hayaitts.data.defaults.DefaultsRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.download.DownloadRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.settings.SettingsRepositoryImpl
import dev.ahmedmohamed.hayaitts.data.voices.VoiceRepositoryImpl
import dev.ahmedmohamed.hayaitts.domain.repo.CatalogRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DefaultsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.DownloadRepository
import dev.ahmedmohamed.hayaitts.domain.repo.SettingsRepository
import dev.ahmedmohamed.hayaitts.domain.repo.VoiceRepository
import dev.ahmedmohamed.hayaitts.ui.library.LibraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

/**
 * Wired up by HayaiTtsApplication.startKoin. Phase 4a adds catalog + voice +
 * download + defaults + settings repositories, plus the LibraryViewModel for
 * the smoke-test button.
 */
val appModule = module {

    // Long-lived application scope used by repositories that need to launch
    // their own coroutines (catalog refresh, download state writes, etc.).
    // SupervisorJob so one failure does not cancel the rest.
    single(named("appScope")) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
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
        )
    }
    single<DownloadRepository> {
        DownloadRepositoryImpl(
            context = androidContext(),
            downloadStateDao = get(),
            settings = get(),
            appScope = get(named("appScope")),
        )
    }
    single<DefaultsRepository> { DefaultsRepositoryImpl(get()) }

    viewModel { LibraryViewModel(get(), get(), get()) }
}
