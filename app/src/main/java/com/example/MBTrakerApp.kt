package com.example

import android.app.Application
import com.example.di.AppContainer

/**
 * Application class managing application-level singleton dependencies and DI container.
 */
class MBTrakerApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        container = AppContainer(this)
    }

    companion object {
        lateinit var instance: MBTrakerApp
            private set
    }
}
