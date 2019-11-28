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

package com.tikalk.worktracker.admin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.navigation.fragment.findNavController
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.net.InternetFragment
import com.tikalk.worktracker.net.TimeTrackerServiceProvider
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_time_list.*
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class ProjectsFragment() : InternetFragment() {

    private val projects: MutableList<Project> = CopyOnWriteArrayList()
    private val listAdapter = ProjectsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_projects, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.adapter = listAdapter
    }

    override fun onStart() {
        super.onStart()
        run()
    }

    @MainThread
    fun run() {
        Timber.v("run")
        showProgress(true)
        loadPage()
            .subscribe({
                bindList(projects)
                //TODO fetchPage()
                showProgress(false)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    private fun loadPage(): Single<Unit> {
        return Single.fromCallable {
            val context: Context = this.context ?: return@fromCallable

            val db = TrackerDatabase.getDatabase(context)
            loadProjects(db)
        }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun loadProjects(db: TrackerDatabase) {
        val projectsDao = db.projectDao()
        val projectsDb = projectsDao.queryAll()
        projects.clear()
        projects.addAll(projectsDb.filter { it.id != TikalEntity.ID_NONE })
    }

    private fun fetchPage() {
        Timber.d("fetchPage")
        // Show a progress spinner, and kick off a background task to perform the user login attempt.
        showProgress(true)

        // Fetch from remote server.
        val service = TimeTrackerServiceProvider.providePlain(context, preferences)

        service.fetchProjects()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    processPage(html)
                    showProgress(false)
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error fetching page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    private fun authenticate(submit: Boolean = false) {
        Timber.v("authenticate submit=$submit")
        val args = Bundle()
        requireFragmentManager().putFragment(args, LoginFragment.EXTRA_CALLER, this)
        args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
        findNavController().navigate(R.id.action_projects_to_login, args)
    }

    private fun processPage(html: String) {
        populateList(html)
        bindList(projects)
    }

    private fun populateList(html: String) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun bindList(projects: List<Project>) {
        listAdapter.submitList(projects)
        if (projects === this.projects) {
            listAdapter.notifyDataSetChanged()
        }
    }
}