package com.riyga.identifier

import android.app.Application
import com.riyga.identifier.di.dataModule
import com.riyga.identifier.di.presentationModule
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