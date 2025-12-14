package by.riyga.shirpid

import android.app.Application
import data.di.dataModule
import by.riyga.shirpid.di.presentationModule
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
            modules(presentationModule, dataModule)
        }
    }
}