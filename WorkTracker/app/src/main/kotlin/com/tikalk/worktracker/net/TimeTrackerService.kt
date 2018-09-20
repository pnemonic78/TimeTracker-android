/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
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

import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.Single
import retrofit2.Response
import retrofit2.http.*

/**
 * Time Tracker web service.
 *
 * @author Moshe Waisberg.
 */
interface TimeTrackerService {

    @FormUrlEncoded
    @POST("login.php")
    fun login(@Field("login") email: String,
              @Field("password") password: String,
              @Field("browser_today") date: String,
              @Field("btn_login") button: String = "Login"): Single<Response<String>>

    @GET("time.php")
    fun fetchTimes(@Query("date") date: String): Single<Response<String>>

    @FormUrlEncoded
    @POST("time.php")
    fun addTime(@Field("project") projectId: Long,
                @Field("task") taskId: Long,
                @Field("date") date: String,
                @Field("start") start: String,
                @Field("finish") finish: String,
                @Field("note") note: String,
                @Field("btn_submit") submit: String = "Submit",
                @Field("browser_today") browserToday: String = formatSystemDate()): Single<Response<String>>

    @GET("time_edit.php")
    fun fetchTimes(@Query("id") id: Long): Single<Response<String>>

    @FormUrlEncoded
    @POST("time_edit.php")
    fun editTime(@Query("id") @Field("id") id: Long,
                 @Field("project") projectId: Long,
                 @Field("task") taskId: Long,
                 @Field("date") date: String,
                 @Field("start") start: String,
                 @Field("finish") finish: String,
                 @Field("note") note: String,
                 @Field("btn_save") submit: String = "Save",
                 @Field("browser_today") browserToday: String = formatSystemDate()): Single<Response<String>>

    @FormUrlEncoded
    @POST("time_delete.php")
    fun deleteTime(@Query("id") @Field("id") id: Long,
                   @Field("delete_button") submit: String = "Delete",
                   @Field("browser_today") browserToday: String = formatSystemDate()): Single<Response<String>>
}