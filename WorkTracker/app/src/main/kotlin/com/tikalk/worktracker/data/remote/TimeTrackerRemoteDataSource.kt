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

import com.tikalk.html.findParentElement
import com.tikalk.worktracker.auth.AuthenticationException
import com.tikalk.worktracker.data.TimeTrackerDataSource
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.net.TimeTrackerService
import io.reactivex.Observable
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import retrofit2.Response

class TimeTrackerRemoteDataSource(private val service: TimeTrackerService) : TimeTrackerDataSource {

    private fun isValidResponse(response: Response<String>): Boolean {
        val html = response.body()
        if (response.isSuccessful && (html != null)) {
            val networkResponse = response.raw().networkResponse()
            val priorResponse = response.raw().priorResponse()
            if ((networkResponse != null) && (priorResponse != null) && priorResponse.isRedirect) {
                val networkUrl = networkResponse.request().url()
                val priorUrl = priorResponse.request().url()
                if (networkUrl == priorUrl) {
                    return true
                }
                when (networkUrl.pathSegments()[networkUrl.pathSize() - 1]) {
                    TimeTrackerService.PHP_TIME,
                    TimeTrackerService.PHP_REPORT ->
                        return true
                }
                return false
            }
            return true
        }
        return false
    }

    override fun projectsPage(): Observable<List<Project>> {
        return service.fetchProjects()
            .map { response ->
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    return@map parseProjectsPage(html)
                }
                throw AuthenticationException("authentication required")
            }
            .toObservable()
    }

    private fun parseProjectsPage(html: String): List<Project> {
        return ProjectsPageParser().parse(html)
    }

    override fun tasksPage(): Observable<List<ProjectTask>> {
        return service.fetchProjectTasks()
            .map { response ->
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    return@map parseProjectTasksPage(html)
                }
                throw AuthenticationException("authentication required")
            }
            .toObservable()
    }

    private fun parseProjectTasksPage(html: String): List<ProjectTask> {
        return ProjectTasksPageParser().parse(html)
    }

    override fun usersPage(): Observable<List<User>> {
        return service.fetchUsers()
            .map { response ->
                if (isValidResponse(response)) {
                    val html = response.body()!!
                    return@map parseUsersPage(html)
                }
                throw AuthenticationException("authentication required")
            }
            .toObservable()
    }

    private fun parseUsersPage(html: String): List<User> {
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

        return users
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

        val roles = if (cols.size > 2) {
            val tdRole = cols[2]
            tdRole.ownText()
        } else ""

        val user = User(username, username, name)
        if (roles.isNotEmpty()) {
            user.roles = roles.split(",")
        }
        user.isUncompletedEntry = isUncompletedEntry
        return user
    }
}