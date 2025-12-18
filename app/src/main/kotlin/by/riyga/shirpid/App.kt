package by.riyga.shirpid

import android.app.Application
import by.riyga.shirpid.presentation.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        initDI()
    }

    private fun initDI() {
        startKoin {
            androidContext(this@App)
            modules(appModules)
        }
    }
}