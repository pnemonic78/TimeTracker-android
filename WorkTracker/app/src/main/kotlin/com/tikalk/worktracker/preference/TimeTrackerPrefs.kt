package com.tikalk.worktracker.preference

import android.content.Context
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials

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
        private const val TASK_ID = "task.id"
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

    fun start(projectId: Long, taskId: Long, startTime: Long) {
        prefs.edit()
            .putLong(PROJECT_ID, projectId)
            .putLong(TASK_ID, taskId)
            .putLong(START_TIME, startTime)
            .apply()
    }
}