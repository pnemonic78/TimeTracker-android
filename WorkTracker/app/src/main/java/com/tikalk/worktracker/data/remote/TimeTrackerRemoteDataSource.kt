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
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.UsersPage
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportFormPage
import com.tikalk.worktracker.model.time.ReportPage
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimerPage
import com.tikalk.worktracker.net.InternetFragmentDelegate.Companion.validateResponse
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.rxjava3.core.Observable
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
        return service.fetchTime(recordId)
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                val page = parseEditPage(html)
                savePage(page)
                return@map page
            }
            .toObservable()
    }

    private fun parseEditPage(html: String): TimeEditPage {
        return TimeEditPageParser().parse(html)
    }

    private fun savePage(page: TimeEditPage) {
        return TimeEditPageSaver(db).save(page)
    }

    override fun projectsPage(refresh: Boolean): Observable<List<Project>> {
        return service.fetchProjects()
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                return@map parseProjectsPage(html)
            }
            .toObservable()
    }

    private fun parseProjectsPage(html: String): List<Project> {
        return ProjectsPageParser().parse(html)
    }

    override fun tasksPage(refresh: Boolean): Observable<List<ProjectTask>> {
        return service.fetchProjectTasks()
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                return@map parseProjectTasksPage(html)
            }
            .toObservable()
    }

    private fun parseProjectTasksPage(html: String): List<ProjectTask> {
        return ProjectTasksPageParser().parse(html)
    }

    override fun usersPage(refresh: Boolean): Observable<UsersPage> {
        return service.fetchUsers()
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                val page = parseUsersPage(html)
                savePage(page)
                return@map page
            }
            .toObservable()
    }

    private fun parseUsersPage(html: String): UsersPage {
        return UsersPageParser().parse(html)
    }

    private fun savePage(page: UsersPage) {
        UserPageSaver(db).save(page)
    }

    override fun reportFormPage(refresh: Boolean): Observable<ReportFormPage> {
        return service.fetchReports()
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                val page = parseReportFormPage(html)
                savePage(page)
                return@map page
            }
            .toObservable()
    }

    private fun parseReportFormPage(html: String): ReportFormPage {
        return ReportFormPageParser().parse(html)
    }

    private fun savePage(page: ReportFormPage) {
        ReportFormPageSaver(db).save(page)
    }

    override fun reportPage(filter: ReportFilter, refresh: Boolean): Observable<ReportPage> {
        return service.generateReport(filter.toFields())
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                val page = parseReportPage(html, filter)
                savePage(page)
                return@map page
            }
            .toObservable()
    }

    private fun parseReportPage(html: String, filter: ReportFilter): ReportPage {
        return ReportPageParser(filter).parse(html, db)
    }

    private fun savePage(page: ReportPage) {
        ReportPageSaver(db).save(page)
    }

    override fun timeListPage(date: Calendar, refresh: Boolean): Observable<TimeListPage> {
        val dateFormatted = formatSystemDate(date).orEmpty()
        return service.fetchTimes(dateFormatted)
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                val page = parseTimeListPage(html)
                savePage(page)
                return@map page
            }
            .toObservable()
    }

    private fun parseTimeListPage(html: String): TimeListPage {
        return TimeListPageParser().parse(html)
    }

    override fun savePage(page: TimeListPage) {
        TimeListPageSaver(db).save(page)
    }

    override fun profilePage(refresh: Boolean): Observable<ProfilePage> {
        return service.fetchProfile()
            .map { response ->
                validateResponse(response)
                val html = response.body()!!
                val page = parseProfilePage(html)
                savePage(page)
                return@map page
            }
            .toObservable()
    }

    private fun parseProfilePage(html: String): ProfilePage {
        return ProfilePageParser().parse(html)
    }

    private fun savePage(page: ProfilePage) {
        ProfilePageSaver(preferences).save(page)
    }

    override fun timerPage(refresh: Boolean): Observable<TimerPage> {
        return Observable.empty()
    }
}