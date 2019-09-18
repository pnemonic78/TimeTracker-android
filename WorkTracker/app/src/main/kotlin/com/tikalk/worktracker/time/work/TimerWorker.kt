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
package com.tikalk.worktracker.time.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import com.tikalk.graphics.drawableToBitmap
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.TimeListActivity
import timber.log.Timber

class TimerWorker(private val context: Context, private val workerParams: Data) {

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
        const val EXTRA_ACTION = BuildConfig.APPLICATION_ID + ".ACTION"

        private const val CHANNEL_ID = "timer"
        private const val ID_NOTIFY = R.string.action_start
        private const val ID_ACTIVITY = 0
        private const val ID_ACTION_STOP = 1

        fun maybeShowNotification(context: Context) {
            Timber.v("maybeShowNotification")
            val prefs = TimeTrackerPrefs(context)
            val record = prefs.readRecord() ?: return
            Timber.v("maybeShowNotification record=$record")
            if (!record.isEmpty()) {
                showNotification(context)
            }
        }

        private fun showNotification(context: Context) {
            Timber.v("showNotification")
            val inputData = Data.Builder()
                .putString(EXTRA_ACTION, ACTION_NOTIFY)
                .putBoolean(EXTRA_NOTIFICATION, true)
                .build()

            val worker = TimerWorker(context, inputData)
            worker.doWork()
        }

        fun hideNotification(context: Context) {
            Timber.v("hideNotification")
            val inputData = Data.Builder()
                .putString(EXTRA_ACTION, ACTION_NOTIFY)
                .putBoolean(EXTRA_NOTIFICATION, false)
                .build()

            val worker = TimerWorker(context, inputData)
            worker.doWork()
        }

        fun startTimer(context: Context, record: TimeRecord) {
            Timber.v("startTimer")
            val inputData = Data.Builder()
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
            Timber.v("stopTimer")
            val extras = intent?.extras
            val inputData = Data.Builder()
                .putString(EXTRA_ACTION, ACTION_STOP)
                .putLong(EXTRA_PROJECT_ID, extras?.getLong(EXTRA_PROJECT_ID) ?: 0L)
                .putLong(EXTRA_TASK_ID, extras?.getLong(EXTRA_TASK_ID) ?: 0L)
                .putLong(EXTRA_START_TIME, extras?.getLong(EXTRA_START_TIME) ?: 0L)
                .putBoolean(EXTRA_EDIT, extras?.getBoolean(EXTRA_EDIT) ?: false)
                .build()

            val worker = TimerWorker(context, inputData)
            worker.doWork()
        }
    }

    private val prefs: TimeTrackerPrefs = TimeTrackerPrefs(context)
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun doWork(): Result {
        val data = workerParams
        return when (data.getString(EXTRA_ACTION)) {
            ACTION_START -> startTimer(data)
            ACTION_STOP -> stopTimer(data)
            ACTION_NOTIFY -> showNotification(data)
            else -> Result.failure()
        }
    }

    private fun startTimer(extras: Data): Result {
        Timber.v("startTimer")
        val record = createRecord(extras) ?: return Result.failure()

        prefs.saveRecord(record)

        if (extras.getBoolean(EXTRA_NOTIFICATION, true)) {
            val nm = NotificationManagerCompat.from(context)
            nm.notify(ID_NOTIFY, createNotification(record))
        }

        return Result.success()
    }

    private fun stopTimer(extras: Data): Result {
        Timber.v("stopTimer")
        if (extras.getBoolean(EXTRA_EDIT, false)) {
            val record = prefs.readRecord()
            val projectId = extras.getLong(EXTRA_PROJECT_ID, record?.project?.id ?: 0L)
            val taskId = extras.getLong(EXTRA_TASK_ID, record?.task?.id ?: 0L)
            val startTime = extras.getLong(EXTRA_START_TIME, record?.startTime ?: 0L)
            val finishTime = extras.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())

            if (projectId <= 0L) return Result.failure()
            if (taskId <= 0L) return Result.failure()
            if (startTime <= 0L) return Result.failure()
            if (finishTime <= startTime) return Result.failure()

            editRecord(projectId, taskId, startTime, finishTime)
        }

        dismissNotification()

        return Result.success()
    }

    private fun editRecord(projectId: Long, taskId: Long, startTime: Long, finishTime: Long) {
        Timber.v("editRecord $projectId,$taskId,$startTime,$finishTime")
        if (projectId <= 0L) return
        if (taskId <= 0L) return
        if (startTime <= 0L) return
        if (finishTime <= startTime) return

        val intent = Intent(context, TimeListActivity::class.java).apply {
            action = TimeListActivity.ACTION_STOP
            addFlags(FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(FLAG_ACTIVITY_NEW_TASK)
            putExtra(TimeListActivity.EXTRA_PROJECT_ID, projectId)
            putExtra(TimeListActivity.EXTRA_TASK_ID, taskId)
            putExtra(TimeListActivity.EXTRA_START_TIME, startTime)
            putExtra(TimeListActivity.EXTRA_FINISH_TIME, finishTime)
        }
        context.startActivity(intent)
    }

    /**
     * Create a notification while this service is running.
     * @return the notification.
     */
    private fun createNotification(record: TimeRecord): Notification {
        Timber.v("createNotification record=$record")
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
        val intent = Intent(context, TimeReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_PROJECT_ID, projectId)
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_START_TIME, startTime)
            putExtra(EXTRA_EDIT, true)
        }
        return PendingIntent.getBroadcast(context, ID_ACTION_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun showNotification(extras: Data): Result {
        val visible = extras.getBoolean(EXTRA_NOTIFICATION, false)
        Timber.v("showNotification visible=$visible")
        if (visible) {
            val record = createRecord(extras) ?: prefs.readRecord() ?: return Result.failure()
            Timber.v("showNotification record=$record")
            val nm = NotificationManagerCompat.from(context)
            nm.notify(ID_NOTIFY, createNotification(record))
        } else {
            dismissNotification()
        }

        return Result.success()
    }

    private fun dismissNotification() {
        Timber.v("dismissNotification")
        val nm = NotificationManagerCompat.from(context)
        nm.cancel(ID_NOTIFY)
    }

    private fun createRecord(extras: Data): TimeRecord? {
        val projectId = extras.getLong(EXTRA_PROJECT_ID, 0L)
        val projectName = extras.getString(EXTRA_PROJECT_NAME)
        val taskId = extras.getLong(EXTRA_TASK_ID, 0L)
        val taskName = extras.getString(EXTRA_TASK_NAME)
        val startTime = extras.getLong(EXTRA_START_TIME, 0L)
        val finishTime = extras.getLong(EXTRA_FINISH_TIME, 0L)
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
        val record = TimeRecord(TikalEntity.ID_NONE, User.EMPTY.copy(), project, task)
        record.startTime = startTime
        record.finishTime = finishTime
        return record
    }
}
