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

import android.content.Context
import com.tikalk.net.PersistentCookieStore
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.net.TimeTrackerService.Companion.BASE_URL
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import okhttp3.JavaNetCookieJar
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level
import retrofit2.Retrofit
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.net.CookieHandler
import java.net.CookieManager

/**
 * Time Tracker web service factory.
 *
 * @author Moshe Waisberg.
 */
class TimeTrackerServiceFactory {

    companion object {
        private var cookieHandlerDefault: CookieManager? = null
        private var cookieHandlerPersistent: CookieManager? = null

        fun createCookieHandler(context: Context?): CookieHandler {
            val cookieHandler: CookieHandler
            if (context == null) {
                if (cookieHandlerDefault == null) {
                    cookieHandlerDefault = CookieManager()
                }
                cookieHandler = cookieHandlerDefault!!
            } else {
                if (cookieHandlerPersistent == null) {
                    cookieHandlerPersistent = CookieManager(PersistentCookieStore(context), null)
                }
                cookieHandler = cookieHandlerPersistent!!
            }
            return cookieHandler
        }

        fun createHttpClient(
            preferences: TimeTrackerPrefs? = null,
            cookieHandler: CookieHandler
        ): OkHttpClient {
            val httpClientBuilder = OkHttpClient.Builder()
                .cookieJar(JavaNetCookieJar(cookieHandler))

            if (BuildConfig.DEBUG) {
                httpClientBuilder.addInterceptor(
                    HttpLoggingInterceptor()
                        .setLevel(Level.BODY)
                )
            }

            if (preferences != null) {
                httpClientBuilder.addInterceptor(AuthenticationInterceptor(preferences))
            }

            return httpClientBuilder.build()
        }

        fun createRetrofit(httpClient: OkHttpClient): Retrofit {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(httpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build()
        }

        fun createTimeTracker(retrofit: Retrofit): TimeTrackerService {
            return retrofit.create(TimeTrackerService::class.java)
        }

        fun clearCookies() {
            cookieHandlerDefault?.cookieStore?.removeAll()
            cookieHandlerPersistent?.cookieStore?.removeAll()
        }
    }
}
