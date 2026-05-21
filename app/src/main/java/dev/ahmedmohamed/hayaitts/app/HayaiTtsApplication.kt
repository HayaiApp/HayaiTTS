package dev.ahmedmohamed.hayaitts.app

import android.app.Application
import dev.ahmedmohamed.hayaitts.app.di.appModule
import dev.ahmedmohamed.hayaitts.data.download.DownloadNotifications
import dev.ahmedmohamed.hayaitts.data.update.UpdateCheckWorker
import dev.ahmedmohamed.hayaitts.tts.HayaiTtsNudge
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HayaiTtsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HayaiTtsApplication)
            modules(appModule)
        }
        // The voice download worker needs the channel before it can call
        // setForeground(); creating it on a cold start (when the worker may
        // run before any other component) avoids racing the system watchdog.
        DownloadNotifications.ensureChannel(this)
        // Phase 8b: ensure the TTS nudge channel exists so HayaiTtsService can
        // post the "speaking" notification immediately on first synthesis.
        HayaiTtsNudge.ensureChannel(this)
        // Phase 8c: schedule the 12h update poll. Idempotent via KEEP policy.
        UpdateCheckWorker.schedule(this)
    }
}
