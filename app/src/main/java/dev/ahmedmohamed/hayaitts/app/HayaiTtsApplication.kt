package dev.ahmedmohamed.hayaitts.app

import android.app.Application
import dev.ahmedmohamed.hayaitts.app.di.appModule
import dev.ahmedmohamed.hayaitts.data.download.DownloadNotifications
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
    }
}
