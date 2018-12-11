package com.tikalk.worktracker.time

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Bundle
import android.os.IBinder
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.preference.TimeTrackerPrefs

class TimerService : IntentService("TimerService") {

    companion object {
        const val ACTION_START = BuildConfig.APPLICATION_ID + ".START"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".PROJECT_ID"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".TASK_ID"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".FINISH_TIME"
    }

    private lateinit var prefs: TimeTrackerPrefs
    private lateinit var notificationManager: NotificationManager

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        val context: Context = this
        prefs = TimeTrackerPrefs(context)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onHandleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START -> startTimer(intent.extras ?: return)
            ACTION_STOP -> stopTimer(intent.extras ?: return)
        }
    }

    private fun startTimer(extras: Bundle) {
        val projectId = extras.getLong(EXTRA_PROJECT_ID)
        if (projectId <= 0L) return
        val taskId = extras.getLong(EXTRA_TASK_ID)
        if (taskId <= 0L) return
        val startTime = extras.getLong(EXTRA_START_TIME)
        if (startTime <= 0L) return

        prefs.startRecord(projectId, taskId, startTime)
    }

    private fun stopTimer(extras: Bundle) {
        val projectId = extras.getLong(EXTRA_PROJECT_ID)
        if (projectId <= 0L) return
        val taskId = extras.getLong(EXTRA_TASK_ID)
        if (taskId <= 0L) return
        val startTime = extras.getLong(EXTRA_START_TIME)
        if (startTime <= 0L) return
        val finishTime = extras.getLong(EXTRA_FINISH_TIME)
        if (finishTime <= startTime) return

        editRecord(projectId, taskId, startTime, finishTime)

        prefs.stopRecord()
    }

    private fun editRecord(projectId: Long, taskId: Long, startTime: Long, finishTime: Long) {
        if (projectId <= 0L) return
        if (taskId <= 0L) return
        if (startTime <= 0L) return
        if (finishTime <= startTime) return

        val context: Context = this
        val intent = Intent(context, TimeEditActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(TimeEditActivity.EXTRA_PROJECT_ID, projectId)
        intent.putExtra(TimeEditActivity.EXTRA_TASK_ID, taskId)
        intent.putExtra(TimeEditActivity.EXTRA_START_TIME, startTime)
        intent.putExtra(TimeEditActivity.EXTRA_FINISH_TIME, finishTime)
        startActivity(intent)
    }
}
