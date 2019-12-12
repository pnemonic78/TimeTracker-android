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

package com.tikalk.worktracker.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.tikalk.app.isNavDestination
import com.tikalk.app.isShowing
import com.tikalk.html.findParentElement
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginFragment
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.net.InternetFragment
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_users.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import timber.log.Timber

class UsersFragment : InternetFragment(),
    LoginFragment.OnLoginListener,
    Observer<List<User>> {

    private val usersData = MutableLiveData<List<User>>()
    private val listAdapter = UsersAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        usersData.observe(this, this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_users, container, false)
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
                fetchPage()
                showProgress(false)
            }, { err ->
                Timber.e(err, "Error loading page: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    private fun loadPage(): Single<Unit> {
        return Single.fromCallable { loadUsers(db) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun loadUsers(db: TrackerDatabase) {
        //TODO implement user table!
        //val usersDao = db.userDao()
        //val usersDb = usersDao.queryAll()
        //val users = usersDb
        //    .filter { it.id != TikalEntity.ID_NONE }
        //    .sortedBy { it.displayName }
        val users = emptyList<User>()
        usersData.postValue(users)
    }

    private fun fetchPage() {
        Timber.d("fetchPage")
        // Show a progress spinner, and kick off a background task to fetch the users.
        showProgress(true)

        // Fetch from remote server.
        service.fetchUsers()
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
                handleError(err)
                showProgress(false)
            })
            .addTo(disposables)
    }

    override fun authenticate(submit: Boolean) {
        Timber.v("authenticate submit=$submit")
        if (!isNavDestination(R.id.loginFragment)) {
            val args = Bundle()
            requireFragmentManager().putFragment(args, LoginFragment.EXTRA_CALLER, this)
            args.putBoolean(LoginFragment.EXTRA_SUBMIT, submit)
            findNavController().navigate(R.id.action_users_to_login, args)
        }
    }

    private fun processPage(html: String) {
        populateList(html)
    }

    private fun populateList(html: String) {
        val doc: Document = Jsoup.parse(html)
        val users = ArrayList<User>()

        // The first row of the table is the header
        val table = findUsersTable(doc)
        if (table != null) {
            // loop through all the rows and parse each record
            // First row is the header, so drop it.
            val rows = table.getElementsByTag("tr").drop(1)
            for (tr in rows) {
                val user = parseUser(tr)
                if (user != null) {
                    users.add(user)
                }
            }
        }

        usersData.postValue(users)
    }

    /**
     * Find the first table whose first row has both class="tableHeader" and labels 'Name' and 'Description'
     */
    private fun findUsersTable(doc: Document): Element? {
        val body = doc.body()
        val candidates = body.select("td[class='tableHeader']")
        var td: Element
        var label: String

        for (candidate in candidates) {
            td = candidate
            label = td.ownText()
            if (label != "Name") {
                continue
            }
            td = td.nextElementSibling() ?: continue
            label = td.ownText()
            if (label != "Login") {
                continue
            }
            td = td.nextElementSibling() ?: continue
            label = td.ownText()
            if (label != "Role") {
                continue
            }
            return findParentElement(td, "table")
        }

        return null
    }

    private fun parseUser(row: Element): User? {
        val cols = row.getElementsByTag("td")

        val tdName = cols[0]
        val name = tdName.ownText()
        val spans = tdName.select("span")
        var isUncompletedEntry = false
        for (span in spans) {
            val classAttribute = span.attr("class")
            isUncompletedEntry = isUncompletedEntry or (classAttribute == "uncompleted-entry active")
        }

        val tdLogin = cols[1]
        val username = tdLogin.ownText()

        val tdRole = cols[2]
        val roles = tdRole.ownText()

        val user = User(username, username, name, null, null, roles.split(","))
        user.isUncompletedEntry = isUncompletedEntry
        return user
    }

    private fun bindList(users: List<User>) {
        listAdapter.submitList(users)
        if (users === this.usersData.value) {
            listAdapter.notifyDataSetChanged()
        }
    }

    override fun onLoginSuccess(fragment: LoginFragment, login: String) {
        Timber.i("login success")
        if (fragment.isShowing()) {
            findNavController().popBackStack()
        }
        run()
    }

    override fun onLoginFailure(fragment: LoginFragment, login: String, reason: String) {
        Timber.e("login failure: $reason")
    }

    override fun onChanged(users: List<User>) {
        bindList(users)
    }
}