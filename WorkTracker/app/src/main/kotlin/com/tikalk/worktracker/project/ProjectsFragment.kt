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

package com.tikalk.worktracker.project

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.net.InternetFragment
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_projects.*
import timber.log.Timber

class ProjectsFragment : InternetFragment() {

    private val projectsData = MutableLiveData<List<Project>>()
    private val listAdapter = ProjectsAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectsData.observe(this, { projects ->
            bindList(projects)
        })
        authenticationViewModel.login.observe(this, { (_, reason) ->
            if (reason == null) {
                Timber.i("login success")
                run()
            } else {
                Timber.e("login failure: $reason")
            }
        })
    }

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
        Timber.i("run first=$firstRun")
        dataSource.projectsPage(firstRun)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ projects ->
                projectsData.value = projects
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                handleError(err)
            })
            .addTo(disposables)
    }

    private fun bindList(projects: List<Project>) {
        listAdapter.submitList(projects)
        if (projects === this.projectsData.value) {
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun authenticate(submit: Boolean) {
        Timber.i("authenticate submit=$submit currentDestination=${findNavController().currentDestination?.label}")
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_projects_to_login, args)
        }
    }
}