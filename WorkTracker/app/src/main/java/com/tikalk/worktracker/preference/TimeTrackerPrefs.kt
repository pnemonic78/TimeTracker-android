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
package com.tikalk.worktracker.preference

import android.content.Context
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.toCalendar

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
        private const val USER_EMAIL = "user.email"
        private const val USER_DISPLAY_NAME = "user.displayName"

        private const val PROJECT_ID = "project.id"
        private const val PROJECT_NAME = "project.name"
        private const val PROJECT_FAVORITE = "project.favorite"
        private const val LOCATION = "location.id"
        private const val LOCATION_FAVORITE = "location.favorite"
        private const val TASK_ID = "task.id"
        private const val TASK_NAME = "task.name"
        private const val TASK_FAVORITE = "task.favorite"
        private const val START_TIME = "start.time"
    }

    var basicCredentials: BasicCredentials = BasicCredentials.EMPTY.copy()
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

    var userCredentials: UserCredentials = UserCredentials.EMPTY.copy()
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

    fun startRecord(
        projectId: Long,
        projectName: String,
        taskId: Long,
        taskName: String,
        startTime: Long,
        remoteId: Long
    ) {
        prefs.edit()
            .putLong(PROJECT_ID, projectId)
            .putString(PROJECT_NAME, projectName)
            .putLong(TASK_ID, taskId)
            .putString(TASK_NAME, taskName)
            .putLong(START_TIME, startTime)
            .putLong(LOCATION, remoteId)
            .apply()
    }

    fun startRecord(record: TimeRecord) {
        startRecord(
            record.project.id,
            record.project.name,
            record.task.id,
            record.task.name,
            record.startTime,
            record.location.id
        )
    }

    fun getStartedRecord(): TimeRecord? {
        val projectId = prefs.getLong(PROJECT_ID, TikalEntity.ID_NONE)
        if (projectId == TikalEntity.ID_NONE) return null

        val projectName = prefs.getString(PROJECT_NAME, null) ?: return null

        val taskId = prefs.getLong(TASK_ID, TikalEntity.ID_NONE)
        if (taskId == TikalEntity.ID_NONE) return null

        val taskName = prefs.getString(TASK_NAME, null) ?: return null

        val startTime = prefs.getLong(START_TIME, TimeRecord.NEVER)
        if (startTime <= TimeRecord.NEVER) return null

        val project = Project(projectName, "")
        project.id = projectId
        val task = ProjectTask(taskName, "")
        task.id = taskId
        project.addTask(task)
        val start = startTime.toCalendar()

        val locationId = prefs.getLong(LOCATION, TikalEntity.ID_NONE)
        val location = Location.valueOf(locationId)

        return TimeRecord(TikalEntity.ID_NONE, project, task, start, location = location)
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

    fun setFavorite(record: TimeRecord) {
        setFavorite(record.project, record.task, record.location)
    }

    fun setFavorite(project: Project, task: ProjectTask, location: Location) {
        setFavorite(project.id, task.id, location.id)
    }

    fun setFavorite(projectId: Long, taskId: Long, remoteId: Long = TikalEntity.ID_NONE) {
        prefs.edit()
            .putLong(PROJECT_FAVORITE, projectId)
            .putLong(TASK_FAVORITE, taskId)
            .putLong(LOCATION_FAVORITE, remoteId)
            .apply()
    }

    fun getFavoriteProject(): Long {
        return prefs.getLong(PROJECT_FAVORITE, TikalEntity.ID_NONE)
    }

    fun getFavoriteTask(): Long {
        return prefs.getLong(TASK_FAVORITE, TikalEntity.ID_NONE)
    }

    fun getFavoriteLocation(): Long {
        return prefs.getLong(LOCATION_FAVORITE, TikalEntity.ID_NONE)
    }

    private var _user: User? = null

    var user: User = User.EMPTY
        get() {
            if (_user == null) {
                val username = prefs.getString(USER_CREDENTIALS_LOGIN, null) ?: ""
                val email = prefs.getString(USER_EMAIL, username)
                val displayName = prefs.getString(USER_DISPLAY_NAME, null)
                val user = User(username, email, displayName)
                _user = user
                field = user
            }
            return field
        }
        set(value) {
            field = value
            prefs.edit()
                .putString(USER_CREDENTIALS_LOGIN, value.username)
                .putString(USER_EMAIL, value.email)
                .putString(USER_DISPLAY_NAME, value.displayName)
                .apply()
        }
}
