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

import com.tikalk.html.findParentElement
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.UsersPage
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class UsersPageParser {
    fun parse(html: String): UsersPage {
        val doc: Document = Jsoup.parse(html)
        return parse(doc)
    }

    private fun parse(doc: Document): UsersPage {
        val users = ArrayList<User>()

        // The first row of the table is the header
        val table = findUsersTable(doc)
        if (table != null) {
            // loop through all the rows and parse each record
            // First row is the header, so drop it.
            val rows = table.getElementsByTag("tr").drop(1)
            var id = 0L
            for (tr in rows) {
                val user = parseUser(tr) ?: continue
                id++
                user.id = id
                users.add(user)
            }
        }

        return UsersPage(users)
    }

    /**
     * Find the first table whose first row has both class="tableHeader" and labels 'Name' and 'Description'
     */
    private fun findUsersTable(doc: Document): Element? {
        val body = doc.body()
        val candidates = body.getElementsByTag("th")
        var th: Element
        var label: String

        for (candidate in candidates) {
            th = candidate
            label = th.ownText()
            if (label != "Name") {
                continue
            }
            th = th.nextElementSibling() ?: continue
            label = th.ownText()
            if (label != "Login") {
                continue
            }
            return findParentElement(th, "table")
        }

        return null
    }

    private fun parseUser(row: Element): User? {
        val cols = row.getElementsByTag("td")
        if (cols.isEmpty()) return null

        val tdName = cols[0]
        val name = tdName.ownText()

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
        return user
    }
}