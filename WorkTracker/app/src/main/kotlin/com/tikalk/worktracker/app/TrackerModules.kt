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

package com.tikalk.worktracker.app

import android.content.Context
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.net.TimeTrackerServiceFactory.Companion.createCookieHandler
import com.tikalk.worktracker.net.TimeTrackerServiceFactory.Companion.createHttpClient
import com.tikalk.worktracker.net.TimeTrackerServiceFactory.Companion.createRetrofit
import com.tikalk.worktracker.net.TimeTrackerServiceFactory.Companion.createTimeTracker
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import okhttp3.OkHttpClient
import org.koin.dsl.module
import retrofit2.Retrofit
import java.net.CookieHandler

val databaseModule = module {
    fun provideDatabase(context: Context): TrackerDatabase {
        return TrackerDatabase.getDatabase(context)
    }

    single { provideDatabase(get()) }
}

val preferencesModule = module {
    fun providePreferences(context: Context): TimeTrackerPrefs {
        return TimeTrackerPrefs(context)
    }

    single { providePreferences(get()) }
}

val apiModule = module {
    fun provideTimeTracker(retrofit: Retrofit): TimeTrackerService {
        return createTimeTracker(retrofit)
    }

    single { provideTimeTracker(get()) }
}

val retrofitModule = module {
    fun provideCookieHandler(context: Context? = null): CookieHandler {
        return createCookieHandler(context)
    }

    fun provideHttpClient(preferences: TimeTrackerPrefs? = null, cookieHandler: CookieHandler): OkHttpClient {
        return createHttpClient(preferences, cookieHandler)
    }

    fun provideRetrofit(httpClient: OkHttpClient): Retrofit {
        return createRetrofit(httpClient)
    }

    single { provideCookieHandler(get()) }
    single { provideHttpClient(get(), get()) }
    single { provideRetrofit(get()) }
}