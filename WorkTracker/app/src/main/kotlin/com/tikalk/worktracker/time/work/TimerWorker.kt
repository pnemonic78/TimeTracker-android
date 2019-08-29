/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tikalk.worktracker.time.work

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tikalk.graphics.drawableToBitmap
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.TimeListActivity
import timber.log.Timber

class TimerWorker : IntentService("Tikal Timer") {

    companion object {
        const val ACTION_START = BuildConfig.APPLICATION_ID + ".START"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP"
        const val ACTION_NOTIFY = BuildConfig.APPLICATION_ID + ".NOTIFY"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".PROJECT_ID"
        const val EXTRA_PROJECT_NAME = BuildConfig.APPLICATION_ID + ".PROJECT_NAME"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".TASK_ID"
        const val EXTRA_TASK_NAME = BuildConfig.APPLICATION_ID + ".TASK_NAME"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".FINISH_TIME"
        const val EXTRA_EDIT = BuildConfig.APPLICATION_ID + ".EDIT"
        const val EXTRA_NOTIFICATION = BuildConfig.APPLICATION_ID + ".NOTIFICATION"

        private const val CHANNEL_ID = "timer"
        private const val ID_NOTIFY = R.string.action_start
        private const val ID_ACTIVITY = 0
        private const val ID_ACTION_STOP = 1

        fun maybeShowNotification(context: Context) {
            Timber.v("maybeShowNotification")
            val prefs = TimeTrackerPrefs(context)
            val record = prefs.getStartedRecord() ?: return
            Timber.v("maybeShowNotification record=$record")
            if (!record.isEmpty()) {
                showNotification(context)
            }
        }

        private fun showNotification(context: Context) {
            Timber.v("showNotification")
            val service = Intent(context, TimerWorker::class.java).apply {
                action = ACTION_NOTIFY
                putExtra(EXTRA_NOTIFICATION, true)
            }
            context.startService(service)
        }

        fun hideNotification(context: Context) {
            Timber.v("hideNotification")
            val service = Intent(context, TimerWorker::class.java).apply {
                action = ACTION_NOTIFY
                putExtra(EXTRA_NOTIFICATION, false)
            }
            context.startService(service)
        }

        fun startTimer(context: Context, record: TimeRecord) {
            val service = Intent(context, TimerWorker::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROJECT_ID, record.project.id)
                putExtra(EXTRA_PROJECT_NAME, record.project.name)
                putExtra(EXTRA_TASK_ID, record.task.id)
                putExtra(EXTRA_TASK_NAME, record.task.name)
                putExtra(EXTRA_START_TIME, record.startTime)
                putExtra(EXTRA_NOTIFICATION, false)
            }
            context.startService(service)
        }

        fun stopTimer(context: Context) {
            val service = Intent(context, TimerWorker::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(service)
        }
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
        Timber.v("onHandleIntent $intent")
        when (intent.action) {
            ACTION_START -> startTimer(intent.extras ?: return)
            ACTION_STOP -> stopTimer(intent.extras ?: return)
            ACTION_NOTIFY -> showNotification(intent.extras ?: return)
        }
    }

    private fun startTimer(extras: Bundle) {
        Timber.v("startTimer")
        val record = createRecord(extras) ?: return

        prefs.startRecord(record)

        if (extras.getBoolean(EXTRA_NOTIFICATION, true)) {
            val nm = NotificationManagerCompat.from(this)
            nm.notify(ID_NOTIFY, createNotification(record))
        }
    }

    private fun stopTimer(extras: Bundle) {
        Timber.v("stopTimer")
        if (extras.getBoolean(EXTRA_EDIT)) {
            val projectId = extras.getLong(EXTRA_PROJECT_ID)
            val taskId = extras.getLong(EXTRA_TASK_ID)
            val startTime = extras.getLong(EXTRA_START_TIME)
            val finishTime = extras.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())

            if (projectId <= 0L) return
            if (taskId <= 0L) return
            if (startTime <= 0L) return
            if (finishTime <= startTime) return

            editRecord(projectId, taskId, startTime, finishTime)
        }

        dismissNotification()
    }

    private fun editRecord(projectId: Long, taskId: Long, startTime: Long, finishTime: Long) {
        Timber.v("editRecord $projectId,$taskId,$startTime,$finishTime")
        if (projectId <= 0L) return
        if (taskId <= 0L) return
        if (startTime <= 0L) return
        if (finishTime <= startTime) return

        val context: Context = this
        val intent = Intent(context, TimeListActivity::class.java).apply {
            action = TimeListActivity.ACTION_STOP
            addFlags(FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            putExtra(TimeListActivity.EXTRA_PROJECT_ID, projectId)
            putExtra(TimeListActivity.EXTRA_TASK_ID, taskId)
            putExtra(TimeListActivity.EXTRA_START_TIME, startTime)
            putExtra(TimeListActivity.EXTRA_FINISH_TIME, finishTime)
        }
        startActivity(intent)
    }

    /**
     * Create a notification while this service is running.
     * @return the notification.
     */
    private fun createNotification(record: TimeRecord): Notification {
        Timber.v("createNotification record=$record")
        val context: Context = this
        val res = context.resources

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                channel = NotificationChannel(
                    CHANNEL_ID,
                    getText(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }
            Timber.v("createNotification channel=$channel")
        }

        val title = res.getText(R.string.title_service)
        val text = res.getString(R.string.notification_description, record.project.name, record.task.name)
        // The PendingIntent to launch our activity if the user selects this notification.
        val contentIntent = createActivityIntent(context)

        val stopActionIntent = createActionIntent(context, ACTION_STOP, record.project.id, record.task.id, record.startTime)
        val stopAction = NotificationCompat.Action(R.drawable.ic_notification_stop, res.getText(R.string.action_stop), stopActionIntent)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setLargeIcon(drawableToBitmap(res, R.mipmap.ic_launcher))
            .setSmallIcon(R.drawable.stat_launcher)  // the status icon
            .setContentTitle(title)  // the label of the entry
            .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setContentText(text)  // the contents of the entry
            .setTicker(text)  // the status text
            .setWhen(record.startTime)  // the time stamp
            .addAction(stopAction)
            .build()
    }

    private fun createActivityIntent(context: Context): PendingIntent {
        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT)
        return PendingIntent.getActivity(context, ID_ACTIVITY, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun createActionIntent(context: Context, action: String, projectId: Long, taskId: Long, startTime: Long): PendingIntent {
        val intent = Intent(context, this.javaClass).apply {
            this.action = action
            putExtra(EXTRA_PROJECT_ID, projectId)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_EDIT, true)
        }
        return PendingIntent.getService(context, ID_ACTION_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun showNotification(extras: Bundle) {
        val visible = extras.getBoolean(EXTRA_NOTIFICATION, false)
        Timber.v("showNotification visible=$visible")
        if (visible) {
            val record = createRecord(extras) ?: prefs.getStartedRecord() ?: return
            Timber.v("showNotification record=$record")
            val nm = NotificationManagerCompat.from(this)
            nm.notify(ID_NOTIFY, createNotification(record))
        } else {
            dismissNotification()
        }
    }

    private fun dismissNotification() {
        Timber.v("dismissNotification")
        val nm = NotificationManagerCompat.from(this)
        nm.cancel(ID_NOTIFY)
    }

    private fun createRecord(extras: Bundle): TimeRecord? {
        val projectId = extras.getLong(EXTRA_PROJECT_ID)
        val projectName = extras.getString(EXTRA_PROJECT_NAME)
        val taskId = extras.getLong(EXTRA_TASK_ID)
        val taskName = extras.getString(EXTRA_TASK_NAME)
        val startTime = extras.getLong(EXTRA_START_TIME)
        val finishTime = extras.getLong(EXTRA_FINISH_TIME)
        Timber.v("createRecord $projectId,$projectName,$taskId,$taskName,$startTime,$finishTime")

        if (projectId <= 0L) return null
        if (projectName.isNullOrEmpty()) return null
        if (taskId <= 0L) return null
        if (taskName.isNullOrEmpty()) return null
        if (startTime <= 0L) return null

        val project = Project(projectName)
        project.id = projectId
        val task = ProjectTask(taskName)
        task.id = taskId
        val record = TimeRecord(User(""), project, task)
        record.startTime = startTime
        record.finishTime = finishTime
        return record
    }
}
