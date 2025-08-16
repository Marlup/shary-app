package com.shary.app.application

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // INITIATE SERVICES
        //DependencyInjector.initAll(this)
        //println("Dependency injections")
    }
}
