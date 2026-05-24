package ru.raydroid

import android.app.Application
import org.koin.android.ext.koin.androidContext
import ru.raydroid.sharedui.initKoin

class RaydroidApp: Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@RaydroidApp)
        }
    }
}
