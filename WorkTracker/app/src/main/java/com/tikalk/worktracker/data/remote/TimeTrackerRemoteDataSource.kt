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

package com.tikalk.worktracker.data.remote

import com.tikalk.worktracker.data.TimeTrackerDataSource
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.ProfilePage
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.UsersPage
import com.tikalk.worktracker.model.time.ProjectTasksPage
import com.tikalk.worktracker.model.time.ProjectsPage
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportFormPage
import com.tikalk.worktracker.model.time.ReportPage
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.PuncherPage
import com.tikalk.worktracker.net.InternetFragmentDelegate.Companion.validateResponse
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.Calendar

class TimeTrackerRemoteDataSource(
    private val service: TimeTrackerService,
    private val db: TrackerDatabase,
    private val preferences: TimeTrackerPrefs
) : TimeTrackerDataSource {

    override fun editPage(recordId: Long, refresh: Boolean): Observable<TimeEditPage> {
        if (recordId == TikalEntity.ID_NONE) {
            return Observable.empty()
        }
        val o = PublishSubject.create<TimeEditPage>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.fetchTime(recordId)
                validateResponse(response)
                val html = response.body()!!
                val page = parseEditPage(html)
                savePage(page)
                o.onNext(page)
                o.onComplete()
            } catch (e: Exception) {
                o.onError(e)
            }
        }
        return o
    }

    private fun parseEditPage(html: String): TimeEditPage {
        return TimeEditPageParser().parse(html)
    }

    private suspend fun savePage(page: TimeEditPage) {
        return TimeEditPageSaver(db).save(page)
    }

    override fun projectsPage(refresh: Boolean): Flow<ProjectsPage> {
        return flow {
            val response = service.fetchProjects()
            validateResponse(response)
            val html = response.body()!!
            val page = parseProjectsPage(html)
            emit(page)
        }
    }

    private fun parseProjectsPage(html: String): ProjectsPage {
        return ProjectsPageParser().parse(html)
    }

    override fun tasksPage(refresh: Boolean): Flow<ProjectTasksPage> {
        return flow {
            val response = service.fetchProjectTasks()
            validateResponse(response)
            val html = response.body()!!
            val page = parseProjectTasksPage(html)
            emit(page)
        }
    }

    private fun parseProjectTasksPage(html: String): ProjectTasksPage {
        return ProjectTasksPageParser().parse(html)
    }

    override fun usersPage(refresh: Boolean): Flow<UsersPage> {
        return flow {
            val response = service.fetchUsers()
            validateResponse(response)
            val html = response.body()!!
            val page = parseUsersPage(html)
            savePage(page)
            emit(page)
        }
    }

    private fun parseUsersPage(html: String): UsersPage {
        return UsersPageParser().parse(html)
    }

    private suspend fun savePage(page: UsersPage) {
        UserPageSaver(db).save(page)
    }

    override fun reportFormPage(refresh: Boolean): Observable<ReportFormPage> {
        val o = PublishSubject.create<ReportFormPage>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.fetchReports()
                validateResponse(response)
                val html = response.body()!!
                val page = parseReportFormPage(html)
                savePage(page)
                o.onNext(page)
                o.onComplete()
            } catch (e: Exception) {
                o.onError(e)
            }
        }
        return o
    }

    private fun parseReportFormPage(html: String): ReportFormPage {
        return ReportFormPageParser().parse(html)
    }

    private suspend fun savePage(page: ReportFormPage) {
        ReportFormPageSaver(db).save(page)
    }

    override fun reportPage(filter: ReportFilter, refresh: Boolean): Observable<ReportPage> {
        val o = PublishSubject.create<ReportPage>()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = service.generateReport(filter.toFields())
                validateResponse(response)
                val html = response.body()!!
                val page = parseReportPage(html, filter)
                savePage(page)
                o.onNext(page)
                o.onComplete()
            } catch (e: Exception) {
                o.onError(e)
            }
        }
        return o
    }

    private suspend fun parseReportPage(html: String, filter: ReportFilter): ReportPage {
        return ReportPageParser(filter).parse(html, db)
    }

    private suspend fun savePage(page: ReportPage) {
        ReportPageSaver(db).save(page)
    }

    override fun timeListPage(date: Calendar, refresh: Boolean): Flow<TimeListPage> {
        return flow {
            val dateFormatted = formatSystemDate(date).orEmpty()
            val response = service.fetchTimes(dateFormatted)
            validateResponse(response)
            val html = response.body()!!
            val page = parseTimeListPage(html)
            savePage(page)
            emit(page)
        }
    }

    private fun parseTimeListPage(html: String): TimeListPage {
        return TimeListPageParser().parse(html)
    }

    override suspend fun savePage(page: TimeListPage) {
        TimeListPageSaver(db).save(page)
    }

    override fun profilePage(refresh: Boolean): Flow<ProfilePage> {
        return flow {
            val response = service.fetchProfile()
            validateResponse(response)
            val html = response.body()!!
            val page = parseProfilePage(html)
            savePage(page)
            emit(page)
        }
    }

    private fun parseProfilePage(html: String): ProfilePage {
        return ProfilePageParser().parse(html)
    }

    private fun savePage(page: ProfilePage) {
        ProfilePageSaver(preferences).save(page)
    }

    override fun puncherPage(refresh: Boolean): Flow<PuncherPage> {
        return emptyFlow()
    }
}
