package com.tikalk.worktracker

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.tikalk.worktracker.time.TimerService
import timber.log.Timber

/**
 * Time tracker application.
 */
class TrackerApplication : Application(), Application.ActivityLifecycleCallbacks {

    var active = 0
        private set

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        registerActivityLifecycleCallbacks(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterActivityLifecycleCallbacks(this)
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityStarted(activity: Activity) {
        active++
        Timber.v("onActivityStarted $activity $active")
        hideNotification()
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle?) {
    }

    override fun onActivityStopped(activity: Activity) {
        active = Math.max(0, active - 1)
        Timber.v("onActivityStopped $activity $active")
        if (active == 0) {
            showNotification()
        }
    }

    override fun onActivityCreated(activity: Activity, state: Bundle?) {
    }

    private fun showNotification() {
        Timber.v("showNotification")
        val context: Context = this
        val service = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_NOTIFY
            putExtra(TimerService.EXTRA_NOTIFICATION, true)
        }
        ContextCompat.startForegroundService(context, service)
    }

    private fun hideNotification() {
        Timber.v("hideNotification")
        val context: Context = this
        val service = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_NOTIFY
            putExtra(TimerService.EXTRA_NOTIFICATION, false)
        }
        stopService(service)
    }
}