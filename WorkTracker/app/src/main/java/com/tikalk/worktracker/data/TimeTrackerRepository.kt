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

package com.tikalk.worktracker.data

import com.tikalk.worktracker.data.local.TimeTrackerLocalDataSource
import com.tikalk.worktracker.data.remote.TimeTrackerRemoteDataSource
import com.tikalk.worktracker.model.ProfilePage
import com.tikalk.worktracker.model.UsersPage
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.ProjectTasksPage
import com.tikalk.worktracker.model.time.ProjectsPage
import com.tikalk.worktracker.model.time.PuncherPage
import com.tikalk.worktracker.model.time.ReportFilter
import com.tikalk.worktracker.model.time.ReportFormPage
import com.tikalk.worktracker.model.time.ReportPage
import com.tikalk.worktracker.model.time.TimeEditPage
import com.tikalk.worktracker.model.time.TimeListPage
import com.tikalk.worktracker.model.time.TimeRecord
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import retrofit2.Response

class TimeTrackerRepository @Inject constructor(
    private val localRepository: TimeTrackerLocalDataSource,
    private val remoteRepository: TimeTrackerRemoteDataSource
) : TimeTrackerDataSource {

    override fun editPage(recordId: Long, refresh: Boolean): Flow<TimeEditPage> {
        if (refresh) {
            return merge(
                localRepository.editPage(recordId, refresh),
                remoteRepository.editPage(recordId, refresh)
            )
        }
        return localRepository.editPage(recordId, refresh)
    }

    override fun profilePage(refresh: Boolean): Flow<ProfilePage> {
        if (refresh) {
            return merge(
                localRepository.profilePage(refresh),
                remoteRepository.profilePage(refresh)
            )
        }
        return localRepository.profilePage(refresh)
    }

    override fun projectsPage(refresh: Boolean): Flow<ProjectsPage> {
        if (refresh) {
            return merge(
                localRepository.projectsPage(refresh),
                remoteRepository.projectsPage(refresh)
            )
        }
        return localRepository.projectsPage(refresh)
    }

    override fun reportFormPage(refresh: Boolean): Flow<ReportFormPage> {
        if (refresh) {
            return merge(
                localRepository.reportFormPage(refresh),
                remoteRepository.reportFormPage(refresh)
            )
        }
        return localRepository.reportFormPage(refresh)
    }

    override fun reportPage(filter: ReportFilter, refresh: Boolean): Flow<ReportPage> {
        if (refresh) {
            return merge(
                localRepository.reportPage(filter, refresh),
                remoteRepository.reportPage(filter, refresh)
            )
        }
        return localRepository.reportPage(filter, refresh)
    }

    override fun tasksPage(refresh: Boolean): Flow<ProjectTasksPage> {
        if (refresh) {
            return merge(
                localRepository.tasksPage(refresh),
                remoteRepository.tasksPage(refresh)
            )
        }
        return localRepository.tasksPage(refresh)
    }

    override fun usersPage(refresh: Boolean): Flow<UsersPage> {
        if (refresh) {
            return merge(
                localRepository.usersPage(refresh),
                remoteRepository.usersPage(refresh)
            )
        }
        return localRepository.usersPage(refresh)
    }

    override fun timeListPage(date: Calendar, refresh: Boolean): Flow<TimeListPage> {
        if (refresh) {
            return merge(
                localRepository.timeListPage(date, refresh),
                remoteRepository.timeListPage(date, refresh)
            )
        }
        return localRepository.timeListPage(date, refresh)
    }

    override fun puncherPage(refresh: Boolean): Flow<PuncherPage> {
        return localRepository.puncherPage(refresh)
    }

    override suspend fun savePage(page: TimeListPage): FormPage<*> {
        return localRepository.savePage(page)
    }

    override fun editRecord(record: TimeRecord): Flow<FormPage<*>> {
        return merge(
            localRepository.editRecord(record),
            remoteRepository.editRecord(record)
        )
    }

    override fun deleteRecord(record: TimeRecord): Flow<FormPage<*>> {
        return merge(
            localRepository.deleteRecord(record),
            remoteRepository.deleteRecord(record)
        )
    }

    override fun editProfile(page: ProfilePage): Flow<ProfilePage> {
        return merge(
            localRepository.editProfile(page),
            remoteRepository.editProfile(page)
        )
    }

    override suspend fun login(name: String, password: String, date: String): Response<String> {
        return remoteRepository.login(name, password, date)
    }
}