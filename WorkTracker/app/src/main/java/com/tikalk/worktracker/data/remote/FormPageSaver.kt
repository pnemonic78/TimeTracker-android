/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2020, Tikal Knowledge, Ltd.
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

import androidx.room.withTransaction
import com.tikalk.worktracker.db.ProjectTaskKey
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.TimeRecord
import timber.log.Timber

open class FormPageSaver<R : TimeRecord, P : FormPage<R>>(protected val db: TrackerDatabase) {

    suspend fun save(page: P): FormPage<*> {
        Timber.i("save page $page")
        var result: FormPage<*> = page
        db.withTransaction {
            result = savePage(db, page)
        }
        return result
    }

    protected open suspend fun savePage(db: TrackerDatabase, page: P): FormPage<*> {
        saveProjects(db, page.projects)
        val tasks = page.projects.flatMap { project -> project.tasks }
        saveTasks(db, tasks)
        saveProjectTaskKeys(db, page.projects)
        return page
    }

    protected open suspend fun saveProjects(db: TrackerDatabase, projects: List<Project>) {
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAll()
        val projectsDbById = projectsDb.associateBy { it.id }

        val projectsToInsert = ArrayList<Project>()
        val projectsToUpdate = ArrayList<Project>()
        val projectsToDelete = ArrayList<Project>(projectsDb)
        for (project in projects) {
            val projectId = project.id
            if (projectsDbById.containsKey(projectId)) {
                projectsToUpdate.add(project)
                projectsToDelete.remove(project)
            } else {
                projectsToInsert.add(project)
            }
        }

        if (projectsToDelete.isNotEmpty()) {
            deleteProjectDependencies(db, projectsToDelete)
            projectsDao.delete(projectsToDelete)
        }

        projectsDao.insert(projectsToInsert)
        projectsDao.update(projectsToUpdate)
    }

    private suspend fun deleteProjectDependencies(
        db: TrackerDatabase,
        projectsToDelete: Collection<Project>
    ) {
        val projectIds = projectsToDelete.map { it.id }

        val projectTasksDao = db.projectTaskKeyDao()
        val recordsDao = db.timeRecordDao()
        val reportsDao = db.reportRecordDao()

        projectTasksDao.deleteProjects(projectIds)
        recordsDao.deleteProjects(projectIds)
        reportsDao.deleteProjects(projectIds)
    }

    protected open suspend fun saveTasks(db: TrackerDatabase, tasks: List<ProjectTask>) {
        val tasksDao = db.taskDao()
        val tasksDb = tasksDao.queryAll()
        val tasksDbById = tasksDb.associateBy { it.id }

        val tasksToInsert = ArrayList<ProjectTask>()
        val tasksToUpdate = ArrayList<ProjectTask>()
        val tasksToDelete = ArrayList<ProjectTask>(tasksDb)
        for (task in tasks) {
            val taskId = task.id
            if (tasksDbById.containsKey(taskId)) {
                tasksToUpdate.add(task)
                tasksToDelete.remove(task)
            } else {
                tasksToInsert.add(task)
            }
        }
        if (tasksToDelete.isNotEmpty()) {
            deleteProjectTaskDependencies(db, tasksToDelete)
            tasksDao.delete(tasksToDelete)
        }

        tasksDao.insert(tasksToInsert)
        tasksDao.update(tasksToUpdate)
    }

    private suspend fun deleteProjectTaskDependencies(
        db: TrackerDatabase,
        tasksToDelete: Collection<ProjectTask>
    ) {
        val taskIds = tasksToDelete.map { it.id }

        val projectTasksDao = db.projectTaskKeyDao()
        val recordsDao = db.timeRecordDao()
        val reportsDao = db.reportRecordDao()

        projectTasksDao.deleteTasks(taskIds)
        recordsDao.deleteTasks(taskIds)
        reportsDao.deleteTasks(taskIds)
    }

    protected open suspend fun saveProjectTaskKeys(db: TrackerDatabase, projects: List<Project>) {
        val keys: List<ProjectTaskKey> = projects.flatMap { project ->
            project.tasks.map { task -> ProjectTaskKey(project.id, task.id) }
        }

        val projectTasksDao = db.projectTaskKeyDao()
        val keysDb = projectTasksDao.queryAll()
        val keysDbById = keysDb.associateBy { it.id }
        val keysToInsert = ArrayList<ProjectTaskKey>()
        val keysToUpdate = ArrayList<ProjectTaskKey>()
        val keysToDelete = ArrayList<ProjectTaskKey>(keysDb)
        for (key in keys) {
            if (keysDbById.containsKey(key.id)) {
                keysToUpdate.add(key)
                keysToDelete.remove(key)
            } else {
                keysToInsert.add(key)
            }
        }

        projectTasksDao.delete(keysToDelete)
        projectTasksDao.insert(keysToInsert)
        projectTasksDao.update(keysToUpdate)
    }
}