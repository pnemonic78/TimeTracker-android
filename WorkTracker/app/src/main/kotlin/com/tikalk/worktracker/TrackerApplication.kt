package com.tikalk.worktracker

import android.app.Application
import timber.log.Timber

/**
 * Time tracker application.
 */
class TrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}