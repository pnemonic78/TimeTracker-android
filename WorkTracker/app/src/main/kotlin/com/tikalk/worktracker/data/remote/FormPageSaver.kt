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

import com.tikalk.worktracker.db.ProjectTaskKey
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.time.FormPage
import com.tikalk.worktracker.model.time.TimeRecord
import timber.log.Timber

open class FormPageSaver<R : TimeRecord, P : FormPage<R>>(protected val db: TrackerDatabase) {

    fun save(page: P) {
        Timber.i("save page $page")
        db.runInTransaction {
            savePage(db, page)
        }
    }

    protected open fun savePage(db: TrackerDatabase, page: P) {
        saveProjects(db, page.projects)
        val tasks = page.projects.flatMap { project -> project.tasks }
        saveTasks(db, tasks)
        saveProjectTaskKeys(db, page.projects)
    }

    protected open fun saveProjects(db: TrackerDatabase, projects: List<Project>) {
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAll()
        val projectsDbById: MutableMap<Long, Project> = HashMap()
        for (project in projectsDb) {
            val projectId = project.id
            projectsDbById[projectId] = project
        }

        val projectsToInsert = ArrayList<Project>()
        val projectsToUpdate = ArrayList<Project>()
        //var projectDb: Project
        for (project in projects) {
            val projectId = project.id
            if (projectsDbById.containsKey(projectId)) {
                //projectDb = projectsDbById[projectId]!!
                //project.dbId = projectDb.dbId
                projectsToUpdate.add(project)
            } else {
                projectsToInsert.add(project)
            }
            projectsDbById.remove(projectId)
        }

        val projectsToDelete = projectsDbById.values
        if (projectsToDelete.isNotEmpty()) {
            deleteProjectDependencies(db, projectsToDelete)
            projectsDao.delete(projectsToDelete)
        }

        if (projectsToInsert.isNotEmpty()) {
            val projectIds = projectsDao.insert(projectsToInsert)
            //for (i in projectIds.indices) {
            //    projectsToInsert[i].dbId = projectIds[i]
            //}
        }

        projectsDao.update(projectsToUpdate)
    }

    private fun deleteProjectDependencies(db: TrackerDatabase, projectsToDelete: Collection<Project>) {
        val projectIds = projectsToDelete.map { it.id }

        val projectTasksDao = db.projectTaskKeyDao()
        val recordsDao = db.timeRecordDao()
        val reportsDao = db.reportRecordDao()

        projectTasksDao.deleteProjects(projectIds)
        recordsDao.deleteProjects(projectIds)
        reportsDao.deleteProjects(projectIds)
    }

    protected open fun saveTasks(db: TrackerDatabase, tasks: List<ProjectTask>) {
        val tasksDao = db.taskDao()
        val tasksDb = tasksDao.queryAll()
        val tasksDbById: MutableMap<Long, ProjectTask> = HashMap()
        for (task in tasksDb) {
            tasksDbById[task.id] = task
        }

        val tasksToInsert = ArrayList<ProjectTask>()
        val tasksToUpdate = ArrayList<ProjectTask>()
        //var taskDb: ProjectTask
        for (task in tasks) {
            val taskId = task.id
            if (tasksDbById.containsKey(taskId)) {
                //taskDb = tasksDbById[taskId]!!
                //task.dbId = taskDb.dbId
                tasksToUpdate.add(task)
            } else {
                tasksToInsert.add(task)
            }
            tasksDbById.remove(taskId)
        }

        val tasksToDelete = tasksDbById.values
        if (tasksToDelete.isNotEmpty()) {
            deleteProjectTaskDependencies(db, tasksToDelete)
            tasksDao.delete(tasksToDelete)
        }

        if (tasksToInsert.isNotEmpty()) {
            val taskIds = tasksDao.insert(tasksToInsert)
            //for (i in taskIds.indices) {
            //    tasksToInsert[i].dbId = taskIds[i]
            //}
        }

        tasksDao.update(tasksToUpdate)
    }

    private fun deleteProjectTaskDependencies(db: TrackerDatabase, tasksToDelete: Collection<ProjectTask>) {
        val taskIds = tasksToDelete.map { it.id }

        val projectTasksDao = db.projectTaskKeyDao()
        val recordsDao = db.timeRecordDao()
        val reportsDao = db.reportRecordDao()

        projectTasksDao.deleteTasks(taskIds)
        recordsDao.deleteTasks(taskIds)
        reportsDao.deleteTasks(taskIds)
    }

    protected open fun saveProjectTaskKeys(db: TrackerDatabase, projects: List<Project>) {
        val keys: List<ProjectTaskKey> = projects.flatMap { project ->
            project.tasks.map { task -> ProjectTaskKey(project.id, task.id) }
        }

        val projectTasksDao = db.projectTaskKeyDao()
        val keysDb = projectTasksDao.queryAll()
        val keysDbMutable = keysDb.toMutableList()
        val keysToInsert = ArrayList<ProjectTaskKey>()
        val keysToUpdate = ArrayList<ProjectTaskKey>()
        var keyDbFound: ProjectTaskKey?
        for (key in keys) {
            keyDbFound = null
            for (keyDb in keysDbMutable) {
                if (key == keyDb) {
                    keyDbFound = keyDb
                    break
                }
            }
            if (keyDbFound != null) {
                //key.dbId = keyDbFound.dbId
                keysToUpdate.add(key)
                keysDbMutable.remove(keyDbFound)
            } else {
                keysToInsert.add(key)
            }
        }

        val keysToDelete = keysDbMutable
        if (keysToDelete.isNotEmpty()) {
            projectTasksDao.delete(keysToDelete)
        }

        if (keysToInsert.isNotEmpty()) {
            val keyIds = projectTasksDao.insert(keysToInsert)
            //for (i in keyIds.indices) {
            //    keysToInsert[i].dbId = keyIds[i]
            //}
        }

        projectTasksDao.update(keysToUpdate)
    }
}