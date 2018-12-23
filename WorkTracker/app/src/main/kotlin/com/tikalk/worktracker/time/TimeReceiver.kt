package com.tikalk.worktracker.time

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

/**
 * Time broadcast receiver.
 */
class TimeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Timber.v("onReceive $intent")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            showNotification(context)
        }
    }

    private fun showNotification(context: Context) {
        val service = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_NOTIFY
            putExtra(TimerService.EXTRA_NOTIFICATION, true)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service)
        } else {
            context.startService(service)
        }
    }
}