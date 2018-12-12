package com.tikalk.worktracker.preference

import android.content.Context
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.*

/**
 * Time Tracker preferences.
 * @author moshe on 2018/05/13.
 */
class TimeTrackerPrefs(context: Context) {

    private val prefs = SecurePreferences.getDefaultSharedPreferences(context)

    companion object {
        private const val BASIC_CREDENTIALS_REALM = "credentials.basic.realm"
        private const val BASIC_CREDENTIALS_USER = "credentials.basic.username"
        private const val BASIC_CREDENTIALS_PASSWORD = "credentials.basic.password"

        private const val USER_CREDENTIALS_LOGIN = "user.login"
        private const val USER_CREDENTIALS_PASSWORD = "user.password"

        private const val PROJECT_ID = "project.id"
        private const val PROJECT_NAME = "project.name"
        private const val TASK_ID = "task.id"
        private const val TASK_NAME = "task.name"
        private const val START_TIME = "start.time"
    }

    var basicCredentials: BasicCredentials = BasicCredentials("", "", "")
        get() {
            field.realm = prefs.getString(BASIC_CREDENTIALS_REALM, null) ?: ""
            field.username = prefs.getString(BASIC_CREDENTIALS_USER, null) ?: ""
            field.password = prefs.getString(BASIC_CREDENTIALS_PASSWORD, null) ?: ""
            return field
        }
        set(value) {
            field.realm = value.realm
            field.username = value.username
            field.password = value.password
            prefs.edit()
                .putString(BASIC_CREDENTIALS_REALM, value.realm)
                .putString(BASIC_CREDENTIALS_USER, value.username)
                .putString(BASIC_CREDENTIALS_PASSWORD, value.password)
                .apply()
        }

    var userCredentials: UserCredentials = UserCredentials("", "")
        get() {
            field.login = prefs.getString(USER_CREDENTIALS_LOGIN, null) ?: ""
            field.password = prefs.getString(USER_CREDENTIALS_PASSWORD, null) ?: ""
            return field
        }
        set(value) {
            field.login = value.login
            field.password = value.password
            prefs.edit()
                .putString(USER_CREDENTIALS_LOGIN, value.login)
                .putString(USER_CREDENTIALS_PASSWORD, value.password)
                .apply()
        }

    fun startRecord(projectId: Long, projectName: String, taskId: Long, taskName: String, startTime: Long) {
        prefs.edit()
            .putLong(PROJECT_ID, projectId)
            .putString(PROJECT_NAME, projectName)
            .putLong(TASK_ID, taskId)
            .putString(TASK_NAME, taskName)
            .putLong(START_TIME, startTime)
            .apply()
    }

    fun startRecord(record: TimeRecord) {
        startRecord(record.project.id,
            record.project.name,
            record.task.id,
            record.task.name,
            record.startTime)
    }

    fun getStartedRecord(): TimeRecord? {
        val projectId = prefs.getLong(PROJECT_ID, 0L)
        if (projectId <= 0L) return null

        val projectName = prefs.getString(PROJECT_NAME, null) ?: return null

        val taskId = prefs.getLong(TASK_ID, 0L)
        if (taskId <= 0L) return null

        val taskName = prefs.getString(TASK_NAME, null) ?: return null

        val startTime = prefs.getLong(START_TIME, 0L)
        if (startTime <= 0L) return null

        val user = User(userCredentials.login)
        val project = Project("")
        project.id = projectId
        project.name = projectName
        project.taskIds += taskId
        val task = ProjectTask("")
        task.id = taskId
        task.name = taskName
        val start = Calendar.getInstance()
        start.timeInMillis = startTime

        return TimeRecord(user, project, task, start)
    }

    fun stopRecord() {
        prefs.edit()
            .remove(PROJECT_ID)
            .remove(PROJECT_NAME)
            .remove(TASK_ID)
            .remove(TASK_NAME)
            .remove(START_TIME)
            .apply()
    }
}