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
package com.tikalk.worktracker.net

import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.time.formatSystemDate
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FieldMap
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Time Tracker web service.
 *
 * @author Moshe Waisberg.
 */
interface TimeTrackerService {

    @FormUrlEncoded
    @POST(PHP_LOGIN)
    suspend fun login(
        @Field("login") name: String,
        @Field("password") password: String,
        @Field("browser_today") date: String,
        @Field("btn_login") button: String = "Login"
    ): Response<String>

    @GET(PHP_LOGOUT)
    suspend fun logout(): Response<String>

    @GET(PHP_TIME)
    suspend fun fetchTimes(@Query("date") date: String): Response<String>

    @FormUrlEncoded
    @POST(PHP_TIME)
    @Headers("Referer: ${BASE_URL}${PHP_TIME}")
    suspend fun addTime(
        @Field("project") projectId: Long,
        @Field("task") taskId: Long,
        @Field("date") date: String,
        @Field("start") start: String?,
        @Field("finish") finish: String?,
        @Field("duration") duration: String?,
        @Field("note") note: String,
        @Field("btn_submit") submit: String = "Submit",
        @Field("browser_today") browserToday: String = formatSystemDate()
    ): Response<String>

    @GET(PHP_EDIT)
    suspend fun fetchTime(@Query("id") id: Long): Response<String>

    @FormUrlEncoded
    @POST(PHP_EDIT)
    @Headers("Referer: ${BASE_URL}${PHP_EDIT}")
    suspend fun editTime(
        @Field("id") id: Long,
        @Field("project") projectId: Long,
        @Field("task") taskId: Long,
        @Field("date") date: String,
        @Field("start") start: String?,
        @Field("finish") finish: String?,
        @Field("duration") duration: String?,
        @Field("note") note: String,
        @Field("btn_save") submit: String = "Save",
        @Field("browser_today") browserToday: String = formatSystemDate()
    ): Response<String>

    @FormUrlEncoded
    @POST(PHP_DELETE)
    @Headers("Referer: ${BASE_URL}${PHP_EDIT}")
    suspend fun deleteTime(
        @Field("id") id: Long,
        @Field("delete_button") submit: String = "Delete",
        @Field("browser_today") browserToday: String = formatSystemDate()
    ): Response<String>

    @GET(PHP_PROFILE)
    suspend fun fetchProfile(): Response<String>

    @FormUrlEncoded
    @POST(PHP_PROFILE)
    @Headers("Referer: ${BASE_URL}${PHP_PROFILE}")
    suspend fun editProfile(
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("login") login: String,
        @Field("password1") password1: String,
        @Field("password2") password2: String,
        @Field("btn_save") submit: String = "Save"
    ): Response<String>

    @GET(PHP_PROJECTS)
    suspend fun fetchProjects(): Response<String>

    @GET(PHP_TASKS)
    suspend fun fetchProjectTasks(): Response<String>

    @GET(PHP_USERS)
    suspend fun fetchUsers(): Response<String>

    @GET(PHP_REPORTS)
    suspend fun fetchReports(): Response<String>

    @FormUrlEncoded
    @POST(PHP_REPORTS)
    @Headers("Referer: ${BASE_URL}${PHP_REPORTS}")
    suspend fun generateReport(
        @FieldMap filter: Map<String, String>,
        @Field("btn_generate") submit: String = "Generate"
    ): Response<String>

    companion object {
        const val BASE_URL = BuildConfig.API_URL

        const val PHP_ACCESS_DENIED = "access_denied.php"
        const val PHP_DELETE = "time_delete.php"
        const val PHP_EDIT = "time_edit.php"
        const val PHP_LOGIN = "login.php"
        const val PHP_LOGOUT = "logout.php"
        const val PHP_PROFILE = "profile_edit.php"
        const val PHP_PROJECTS = "projects.php"
        const val PHP_REPORT = "report.php"
        const val PHP_REPORTS = "reports.php"
        const val PHP_TASKS = "tasks.php"
        const val PHP_TIME = "time.php"
        const val PHP_USERS = "users.php"
    }
}