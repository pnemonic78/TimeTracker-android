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
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

interface TimeTrackerDataSource {
    fun editPage(recordId: Long, refresh: Boolean = true): Flow<TimeEditPage>
    fun profilePage(refresh: Boolean = true): Flow<ProfilePage>
    fun projectsPage(refresh: Boolean = true): Flow<ProjectsPage>
    fun puncherPage(refresh: Boolean = true): Flow<PuncherPage>
    fun reportFormPage(refresh: Boolean = true): Flow<ReportFormPage>
    fun reportPage(filter: ReportFilter, refresh: Boolean = true): Flow<ReportPage>
    fun tasksPage(refresh: Boolean = true): Flow<ProjectTasksPage>
    fun timeListPage(date: Calendar, refresh: Boolean = true): Flow<TimeListPage>
    fun usersPage(refresh: Boolean = true): Flow<UsersPage>

    suspend fun savePage(page: TimeListPage): FormPage<*>
    suspend fun editRecord(record: TimeRecord): FormPage<*>
}