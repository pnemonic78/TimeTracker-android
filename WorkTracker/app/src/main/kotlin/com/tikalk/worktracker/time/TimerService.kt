package com.tikalk.worktracker.time

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.preference.TimeTrackerPrefs

class TimerService : Service() {

    companion object {
        const val ACTION_START = BuildConfig.APPLICATION_ID + ".START"
        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".STOP"

        const val EXTRA_PROJECT_ID = BuildConfig.APPLICATION_ID + ".PROJECT_ID"
        const val EXTRA_PROJECT_NAME = BuildConfig.APPLICATION_ID + ".PROJECT_NAME"
        const val EXTRA_TASK_ID = BuildConfig.APPLICATION_ID + ".TASK_ID"
        const val EXTRA_TASK_NAME = BuildConfig.APPLICATION_ID + ".TASK_NAME"
        const val EXTRA_START_TIME = BuildConfig.APPLICATION_ID + ".START_TIME"
        const val EXTRA_FINISH_TIME = BuildConfig.APPLICATION_ID + ".FINISH_TIME"
        const val EXTRA_EDIT = BuildConfig.APPLICATION_ID + ".EDIT"

        private const val CHANNEL_ID = "timer"
        private const val ID_NOTIFY = R.string.action_start
        private const val ID_ACTIVITY = 0
        private const val ID_ACTION_STOP = 1
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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        onHandleIntent(intent)
        return START_STICKY
    }

    private fun onHandleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START -> startTimer(intent.extras ?: return)
            ACTION_STOP -> stopTimer(intent.extras ?: return)
        }
    }

    private fun startTimer(extras: Bundle) {
        val projectId = extras.getLong(EXTRA_PROJECT_ID)
        if (projectId <= 0L) return
        val projectName = extras.getString(EXTRA_PROJECT_NAME)
        if (projectName.isNullOrEmpty()) return
        val taskId = extras.getLong(EXTRA_TASK_ID)
        if (taskId <= 0L) return
        val taskName = extras.getString(EXTRA_TASK_NAME)
        if (taskName.isNullOrEmpty()) return
        val startTime = extras.getLong(EXTRA_START_TIME)
        if (startTime <= 0L) return

        prefs.startRecord(projectId, projectName, taskId, taskName, startTime)

        startForeground(ID_NOTIFY, createNotification(projectId, projectName, taskId, taskName, startTime))
    }

    private fun stopTimer(extras: Bundle) {
        if (extras.getBoolean(EXTRA_EDIT)) {
            val projectId = extras.getLong(EXTRA_PROJECT_ID)
            if (projectId <= 0L) return
            val taskId = extras.getLong(EXTRA_TASK_ID)
            if (taskId <= 0L) return
            val startTime = extras.getLong(EXTRA_START_TIME)
            if (startTime <= 0L) return
            val finishTime = extras.getLong(EXTRA_FINISH_TIME, System.currentTimeMillis())
            if (finishTime <= startTime) return

            editRecord(projectId, taskId, startTime, finishTime)
        }

        prefs.stopRecord()

        stopForeground(true)
    }

    private fun editRecord(projectId: Long, taskId: Long, startTime: Long, finishTime: Long) {
        if (projectId <= 0L) return
        if (taskId <= 0L) return
        if (startTime <= 0L) return
        if (finishTime <= startTime) return

        val context: Context = this
        val intent = Intent(context, TimeEditActivity::class.java)
        intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.putExtra(TimeEditActivity.EXTRA_PROJECT_ID, projectId)
        intent.putExtra(TimeEditActivity.EXTRA_TASK_ID, taskId)
        intent.putExtra(TimeEditActivity.EXTRA_START_TIME, startTime)
        intent.putExtra(TimeEditActivity.EXTRA_FINISH_TIME, finishTime)
        startActivity(intent)
    }

    /**
     * Create a notification while this service is running.
     * @return the notification.
     */
    private fun createNotification(projectId: Long, projectName: String, taskId: Long, taskName: String, startTime: Long): Notification {
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
        }

        val title = res.getText(R.string.title_service)
        val text = res.getString(R.string.notification_description, projectName, taskName)
        // The PendingIntent to launch our activity if the user selects this notification.
        val contentIntent = createActivityIntent(context)

        val stopActionIntent = createActionIntent(context, ACTION_STOP, projectId, taskId, startTime)
        val stopAction = NotificationCompat.Action(R.drawable.ic_notification_stop, res.getText(R.string.action_stop), stopActionIntent)

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
            .setSmallIcon(R.drawable.stat_launcher)  // the status icon
            .setContentTitle(title)  // the label of the entry
            .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
            .setUsesChronometer(true)
            .setShowWhen(true)
            .setContentText(text)  // the contents of the entry
            .setTicker(text)  // the status text
            .setWhen(startTime)  // the time stamp
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
        val intent = Intent(context, this.javaClass)
        intent.action = action
        intent.putExtra(EXTRA_PROJECT_ID, projectId)
        intent.putExtra(EXTRA_TASK_ID, taskId)
        intent.putExtra(EXTRA_START_TIME, startTime)
        intent.putExtra(EXTRA_EDIT, true)
        return PendingIntent.getService(context, ID_ACTION_STOP, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
}
