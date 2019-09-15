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
package com.tikalk.worktracker.time

import android.os.Bundle
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.*

abstract class TimeFormActivity : InternetActivity() {

    protected var date
        get() = formFragment.date
        set(value) {
            formFragment.date = value
        }
    protected var user
        get() = formFragment.user
        set(value) {
            formFragment.user = value
        }
    protected var record
        get() = formFragment.record
        set(value) {
            formFragment.record = value
        }
    protected val projects
        get() = formFragment.projects
    protected val tasks
        get() = formFragment.tasks
    protected var projectEmpty
        get() = formFragment.projectEmpty
        set(value) {
            formFragment.projectEmpty = value
        }
    protected var taskEmpty
        get() = formFragment.taskEmpty
        set(value) {
            formFragment.taskEmpty = value
        }
    protected val records
        get() = formFragment.records
    protected lateinit var formFragment: TimeFormFragment

    protected lateinit var prefs: TimeTrackerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)
    }

    protected fun findSelectedProject(project: Element, projects: List<Project>): Project {
        return formFragment.findSelectedProject(project, projects)
    }

    protected fun findSelectedTask(task: Element, tasks: List<ProjectTask>): ProjectTask {
        return formFragment.findSelectedTask(task, tasks)
    }

    protected fun populateProjects(select: Element, target: MutableList<Project>) {
        formFragment.populateProjects(select, target)
    }

    protected fun populateTasks(select: Element, target: MutableList<ProjectTask>) {
        formFragment.populateTasks(select, target)
    }

    protected fun populateTaskIds(doc: Document, projects: List<Project>) {
        formFragment.populateTaskIds(doc, projects)
    }

    protected fun markFavorite() {
        prefs.setFavorite(record)
    }

    /**
     * Shows the progress UI and hides the login form.
     * @param show visible?
     */
    abstract fun showProgress(show: Boolean)

    protected fun showProgressMain(show: Boolean) {
        runOnUiThread { showProgress(show) }
    }

    protected open fun saveFormToDb() {
        formFragment.saveFormToDb()
    }

    protected fun loadFormFromDb() {
        formFragment.loadFormFromDb()
    }

    protected open fun saveRecords(db: TrackerDatabase, day: Calendar?) {
        formFragment.saveRecords(db, day)
    }
}