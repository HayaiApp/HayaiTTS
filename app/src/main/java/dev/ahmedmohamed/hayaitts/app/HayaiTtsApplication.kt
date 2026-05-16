package dev.ahmedmohamed.hayaitts.app

import android.app.Application
import dev.ahmedmohamed.hayaitts.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class HayaiTtsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@HayaiTtsApplication)
            modules(appModule)
        }
    }
}
