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
import android.content.SharedPreferences
import android.icu.util.Calendar
import androidx.preference.PreferenceManager
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.model.Location
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.time.copy
import com.tikalk.worktracker.time.toCalendar

/**
 * Time Tracker preferences.
 * @author moshe on 2018/05/13.
 */
class TimeTrackerPrefs(context: Context) {

    private val securePreferences: SharedPreferences
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    init {
        securePreferences = try {
            SecurePreferences.getDefaultSharedPreferences(context)
        } catch (e: Exception) {
            PreferenceManager.getDefaultSharedPreferences(context)
        }
    }

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

        private const val HOURS_PER_DAY = "hours_per_day"
    }

    var basicCredentials: BasicCredentials = BasicCredentials.EMPTY.copy()
        get() {
            field.realm = securePreferences.getString(BASIC_CREDENTIALS_REALM, null) ?: ""
            field.username = securePreferences.getString(BASIC_CREDENTIALS_USER, null) ?: ""
            field.password = securePreferences.getString(BASIC_CREDENTIALS_PASSWORD, null) ?: ""
            return field
        }
        set(value) {
            field.realm = value.realm
            field.username = value.username
            field.password = value.password
            securePreferences.edit()
                .putString(BASIC_CREDENTIALS_REALM, value.realm)
                .putString(BASIC_CREDENTIALS_USER, value.username)
                .putString(BASIC_CREDENTIALS_PASSWORD, value.password)
                .apply()
        }

    var userCredentials: UserCredentials = UserCredentials.EMPTY.copy()
        get() {
            field.login = securePreferences.getString(USER_CREDENTIALS_LOGIN, null) ?: ""
            field.password = securePreferences.getString(USER_CREDENTIALS_PASSWORD, null) ?: ""
            return field
        }
        set(value) {
            field.login = value.login
            field.password = value.password
            securePreferences.edit()
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
        securePreferences.edit()
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
        val projectId = securePreferences.getLong(PROJECT_ID, TikalEntity.ID_NONE)
        if (projectId == TikalEntity.ID_NONE) return null

        val projectName = securePreferences.getString(PROJECT_NAME, null) ?: return null

        val taskId = securePreferences.getLong(TASK_ID, TikalEntity.ID_NONE)
        if (taskId == TikalEntity.ID_NONE) return null

        val taskName = securePreferences.getString(TASK_NAME, null) ?: return null

        val startTime = securePreferences.getLong(START_TIME, TimeRecord.NEVER)
        if (startTime <= TimeRecord.NEVER) return null

        val project = Project(name = projectName)
        project.id = projectId
        val task = ProjectTask(name = taskName)
        task.id = taskId
        project.addTask(task)
        val start = startTime.toCalendar()

        val locationId = securePreferences.getLong(LOCATION, TikalEntity.ID_NONE)
        val location = Location.valueOf(locationId)

        return TimeRecord(
            id = TikalEntity.ID_NONE,
            project = project,
            task = task,
            start = start,
            date = start.copy(),
            location = location
        )
    }

    fun stopRecord() {
        securePreferences.edit()
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
        securePreferences.edit()
            .putLong(PROJECT_FAVORITE, projectId)
            .putLong(TASK_FAVORITE, taskId)
            .putLong(LOCATION_FAVORITE, remoteId)
            .apply()
    }

    fun getFavoriteProject(): Long {
        return securePreferences.getLong(PROJECT_FAVORITE, TikalEntity.ID_NONE)
    }

    fun getFavoriteTask(): Long {
        return securePreferences.getLong(TASK_FAVORITE, TikalEntity.ID_NONE)
    }

    fun getFavoriteLocation(): Long {
        return securePreferences.getLong(LOCATION_FAVORITE, TikalEntity.ID_NONE)
    }

    private var _user: User? = null

    var user: User = User.EMPTY
        get() {
            if (_user == null) {
                val username = securePreferences.getString(USER_CREDENTIALS_LOGIN, null) ?: ""
                val email = securePreferences.getString(USER_EMAIL, username)
                val displayName = securePreferences.getString(USER_DISPLAY_NAME, null)
                val user = User(username, email, displayName)
                _user = user
                field = user
            }
            return field
        }
        set(value) {
            field = value
            securePreferences.edit()
                .putString(USER_CREDENTIALS_LOGIN, value.username)
                .putString(USER_EMAIL, value.email)
                .putString(USER_DISPLAY_NAME, value.displayName)
                .apply()
        }

    private val hoursPerDayDefault =
        context.resources.getInteger(R.integer.pref_hours_per_day_defaultValue)

    var workHoursPerDay: Int
        get() = sharedPreferences.getInt(HOURS_PER_DAY, hoursPerDayDefault)
        set(value) {
            sharedPreferences.edit().putInt(HOURS_PER_DAY, value)
        }

    private val isWorkDayDefault = listOf<Boolean>(
        context.resources.getBoolean(R.bool.pref_work_day_sunday_defaultValue),
        context.resources.getBoolean(R.bool.pref_work_day_monday_defaultValue),
        context.resources.getBoolean(R.bool.pref_work_day_tuesday_defaultValue),
        context.resources.getBoolean(R.bool.pref_work_day_wednesday_defaultValue),
        context.resources.getBoolean(R.bool.pref_work_day_thursday_defaultValue),
        context.resources.getBoolean(R.bool.pref_work_day_friday_defaultValue),
        context.resources.getBoolean(R.bool.pref_work_day_saturday_defaultValue)
    )

    val isWorkDaySunday: Boolean
        get() = sharedPreferences.getBoolean("work_day.sunday", isWorkDayDefault[0])
    val isWorkDayMonday: Boolean
        get() = sharedPreferences.getBoolean("work_day.monday", isWorkDayDefault[1])
    val isWorkDayTuesday: Boolean
        get() = sharedPreferences.getBoolean("work_day.tuesday", isWorkDayDefault[2])
    val isWorkDayWednesday: Boolean
        get() = sharedPreferences.getBoolean("work_day.wednesday", isWorkDayDefault[3])
    val isWorkDayThursday: Boolean
        get() = sharedPreferences.getBoolean("work_day.thursday", isWorkDayDefault[4])
    val isWorkDayFriday: Boolean
        get() = sharedPreferences.getBoolean("work_day.friday", isWorkDayDefault[5])
    val isWorkDaySaturday: Boolean
        get() = sharedPreferences.getBoolean("work_day.saturday", isWorkDayDefault[6])

    fun calendarWorkDays(): Collection<Int> {
        val list = mutableListOf<Int>()
        if (isWorkDaySunday) list.add(Calendar.SUNDAY)
        if (isWorkDayMonday) list.add(Calendar.MONDAY)
        if (isWorkDayTuesday) list.add(Calendar.TUESDAY)
        if (isWorkDayWednesday) list.add(Calendar.WEDNESDAY)
        if (isWorkDayThursday) list.add(Calendar.THURSDAY)
        if (isWorkDayFriday) list.add(Calendar.FRIDAY)
        if (isWorkDaySaturday) list.add(Calendar.SATURDAY)
        return list
    }

}
