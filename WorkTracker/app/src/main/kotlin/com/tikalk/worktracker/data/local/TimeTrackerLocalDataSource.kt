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

package com.tikalk.worktracker.data.local

import com.tikalk.worktracker.data.TimeTrackerDataSource
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.User
import io.reactivex.Observable

class TimeTrackerLocalDataSource(private val db: TrackerDatabase) : TimeTrackerDataSource {

    override fun projectsPage(): Observable<List<Project>> {
        return loadProjects(db)
    }

    private fun loadProjects(db: TrackerDatabase): Observable<List<Project>> {
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAllSingle()
        return projectsDb
            .map { projects ->
                projects
                    .filter { it.id != TikalEntity.ID_NONE }
                    .sortedBy { it.name }
            }
            .toObservable()
    }

    override fun tasksPage(): Observable<List<ProjectTask>> {
        return loadTasks(db)
    }

    private fun loadTasks(db: TrackerDatabase): Observable<List<ProjectTask>> {
        val tasksDao = db.taskDao()
        val tasksDb = tasksDao.queryAllSingle()
        return tasksDb
            .map { tasks ->
                tasks
                    .filter { it.id != TikalEntity.ID_NONE }
                    .sortedBy { it.name }
            }
            .toObservable()
    }

    override fun usersPage(): Observable<List<User>> {
        return Observable.empty()
    }
}