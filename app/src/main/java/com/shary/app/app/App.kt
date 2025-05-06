package com.shary.app.app

import android.app.Application
import com.shary.app.core.dependencyContainer.DependencyContainer

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // INITIATE SERVICES
        DependencyContainer.initAll(this)
        System.out.println("Dependency injections")
    }
}
