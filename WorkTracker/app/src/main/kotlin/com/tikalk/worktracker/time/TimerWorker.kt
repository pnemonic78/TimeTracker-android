/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
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
package com.tikalk.worktracker.time

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.text.format.DateUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tikalk.graphics.drawableToBitmap
import com.tikalk.os.BundleBuilder
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import timber.log.Timber

class TimerWorker(private val context: Context, private val workerParams: Bundle) {

    companion object {
        const val ACTION_START = BuildConfig.APPLICATION_ID + ".action.START"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP"
        const val ACTION_NOTIFY = BuildConfig.APPLICATION_ID + ".action.NOTIFY"
        const val ACTION_LAUNCH = BuildConfig.APPLICATION_ID + ".action.LAUNCH"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".PROJECT_ID"
        const val EXTRA_PROJECT_NAME = BuildConfig.APPLICATION_ID + ".PROJECT_NAME"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".TASK_ID"
        const val EXTRA_TASK_NAME = BuildConfig.APPLICATION_ID + ".TASK_NAME"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".FINISH_TIME"
        const val EXTRA_EDIT = BuildConfig.APPLICATION_ID + ".EDIT"
        const val EXTRA_NOTIFICATION = BuildConfig.APPLICATION_ID + ".NOTIFICATION"
        private const val EXTRA_ACTION = BuildConfig.APPLICATION_ID + ".ACTION"

        private const val CHANNEL_ID = "timer"
        private const val ID_NOTIFY = R.string.action_start
        private const val ID_ACTIVITY = 0
        private const val ID_ACTION_STOP = 1

        fun maybeShowNotification(context: Context) {
            Timber.i("maybeShowNotification")
            val prefs = TimeTrackerPrefs(context)
            val record = prefs.getStartedRecord() ?: return
            Timber.i("maybeShowNotification record=$record")
            if (!record.isEmpty()) {
                showNotification(context)
            }
        }

        private fun showNotification(context: Context) {
            Timber.i("showNotification")
            val inputData = BundleBuilder()
                .putString(EXTRA_ACTION, ACTION_NOTIFY)
                .putBoolean(EXTRA_NOTIFICATION, true)
                .build()

            val worker = TimerWorker(context, inputData)
            worker.doWork()
        }

        fun hideNotification(context: Context) {
            Timber.i("hideNotification")
            val inputData = BundleBuilder()
                .putString(EXTRA_ACTION, ACTION_NOTIFY)
                .putBoolean(EXTRA_NOTIFICATION, false)
                .build()

            val worker = TimerWorker(context, inputData)
            worker.doWork()
        }

        fun startTimer(context: Context, record: TimeRecord) {
            Timber.i("startTimer")
            val inputData = BundleBuilder()
                .putString(EXTRA_ACTION, ACTION_START)
                .putLong(EXTRA_PROJECT_ID, record.project.id)
                .putString(EXTRA_PROJECT_NAME, record.project.name)
                .putLong(EXTRA_TASK_ID, record.task.id)
                .putString(EXTRA_TASK_NAME, record.task.name)
                .putLong(EXTRA_START_TIME, record.startTime)
                .putBoolean(EXTRA_NOTIFICATION, false)
                .build()

            val worker = TimerWorker(context, inputData)
            worker.doWork()
        }

        fun stopTimer(context: Context, intent: Intent? = null) {
            Timber.i("stopTimer $intent")
            val extras = intent?.extras
            val inputData = BundleBuilder()
                .putString(EXTRA_ACTION, ACTION_STOP)
                .putBoolean(EXTRA_EDIT, extras?.getBoolean(EXTRA_EDIT) ?: false)
                .build()

            val worker = TimerWorker(context, inputData)
            worker.doWork()
        }

        fun launchApp(context: Context) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(context.packageName) ?: return
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        fun restartApp(context: Context) {
            val intent = Intent(context, TimeReceiver::class.java)
            intent.action = ACTION_LAUNCH
            val operation = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + (2 * DateUtils.SECOND_IN_MILLIS), operation)
        }
    }

    private val prefs: TimeTrackerPrefs = TimeTrackerPrefs(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun doWork(): Result<Any> {
        val data = workerParams
        return when (data.getString(EXTRA_ACTION)) {
            ACTION_START -> startTimerAction(data)
            ACTION_STOP -> stopTimerAction(data)
            ACTION_NOTIFY -> showNotification(data)
            else -> Result.failure(IllegalArgumentException("invalid action"))
        }
    }

    private fun startTimerAction(extras: Bundle): Result<Any> {
        Timber.i("startTimerAction")
        val record = createRecord(extras) ?: return Result.failure(IllegalArgumentException("missing record"))

        prefs.startRecord(record)

        if (extras.getBoolean(EXTRA_NOTIFICATION, true)) {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(ID_NOTIFY, createNotification(record))
        }

        return Result.success(true)
    }

    private fun stopTimerAction(extras: Bundle): Result<Any> {
        Timber.i("stopTimerAction")
        if (extras.getBoolean(EXTRA_EDIT, false)) {
            val record = prefs.getStartedRecord() ?: return Result.failure(IllegalArgumentException("missing record"))
            val projectId = record.project.id
            val taskId = record.task.id
            val startTime = record.startTime

            if (projectId <= 0L) return Result.failure(IllegalArgumentException("invalid project id"))
            if (taskId <= 0L) return Result.failure(IllegalArgumentException("invalid task id"))
            if (startTime <= TimeRecord.NEVER) return Result.failure(IllegalArgumentException("invalid start time"))

            editStartedRecord(record)
        }

        dismissNotification()

        return Result.success(true)
    }

    private fun editStartedRecord(record: TimeRecord) {
        Timber.i("editStartedRecord record=$record")

        val intent = Intent(context, TimeListActivity::class.java).apply {
            action = TimeListActivity.ACTION_STOP
            addFlags(FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Create a notification while this service is running.
     * @return the notification.
     */
    private fun createNotification(record: TimeRecord): Notification {
        Timber.i("createNotification record=$record")
        val res = context.resources

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel: NotificationChannel? = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                channel = NotificationChannel(
                    CHANNEL_ID,
                    context.getText(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }
            Timber.i("createNotification channel=$channel")
        }

        val title = record.project.name
        val text = record.task.name
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
        val intent = Intent(context, TimeReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_PROJECT_ID, projectId)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_EDIT, true)
        }
        return PendingIntent.getBroadcast(context, ID_ACTION_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun showNotification(extras: Bundle): Result<Any> {
        val visible = extras.getBoolean(EXTRA_NOTIFICATION, false)
        Timber.i("showNotification visible=$visible")
        if (visible) {
            val record = createRecord(extras) ?: prefs.getStartedRecord() ?: return Result.failure(IllegalArgumentException("missing record"))
            Timber.i("showNotification record=$record")
            val nm = NotificationManagerCompat.from(context)
            nm.notify(ID_NOTIFY, createNotification(record))
        } else {
            dismissNotification()
        }

        return Result.success(true)
    }

    private fun dismissNotification() {
        Timber.i("dismissNotification")
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(ID_NOTIFY)
    }

    private fun createRecord(extras: Bundle): TimeRecord? {
        val projectId = extras.getLong(EXTRA_PROJECT_ID, 0L)
        val projectName = extras.getString(EXTRA_PROJECT_NAME)
        val taskId = extras.getLong(EXTRA_TASK_ID, 0L)
        val taskName = extras.getString(EXTRA_TASK_NAME)
        val startTime = extras.getLong(EXTRA_START_TIME, TimeRecord.NEVER)
        val finishTime = extras.getLong(EXTRA_FINISH_TIME, TimeRecord.NEVER)
        Timber.i("createRecord $projectId,$projectName,$taskId,$taskName,$startTime,$finishTime")

        if (projectId <= 0L) return null
        if (projectName.isNullOrEmpty()) return null
        if (taskId <= 0L) return null
        if (taskName.isNullOrEmpty()) return null
        if (startTime <= TimeRecord.NEVER) return null

        val project = Project(projectName)
        project.id = projectId
        val task = ProjectTask(taskName)
        task.id = taskId
        val record = TimeRecord(TikalEntity.ID_NONE, project, task)
        record.startTime = startTime
        record.finishTime = finishTime
        return record
    }
}
