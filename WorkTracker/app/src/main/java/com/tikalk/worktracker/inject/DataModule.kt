/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022, Tikal Knowledge, Ltd.
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

package com.tikalk.worktracker.inject

import android.content.Context
import com.tikalk.worktracker.app.TrackerServices
import com.tikalk.worktracker.data.TimeTrackerRepository
import com.tikalk.worktracker.data.local.TimeTrackerLocalDataSource
import com.tikalk.worktracker.data.remote.TimeTrackerRemoteDataSource
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.net.TimeTrackerService
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): TimeTrackerPrefs {
        return TimeTrackerPrefs(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TrackerDatabase {
        return TrackerDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideLocalDataSource(
        db: TrackerDatabase,
        preferences: TimeTrackerPrefs
    ): TimeTrackerLocalDataSource {
        return TimeTrackerLocalDataSource(db, preferences)
    }

    @Provides
    @Singleton
    fun provideRemoteDataSource(
        service: TimeTrackerService,
        db: TrackerDatabase,
        preferences: TimeTrackerPrefs
    ): TimeTrackerRemoteDataSource {
        return TimeTrackerRemoteDataSource(service, db, preferences)
    }

    @Provides
    @Singleton
    fun provideRepository(
        local: TimeTrackerLocalDataSource,
        remote: TimeTrackerRemoteDataSource
    ): TimeTrackerRepository {
        return TimeTrackerRepository(local, remote)
    }

    @Provides
    @Singleton
    fun provideServices(
        preferences: TimeTrackerPrefs,
        db: TrackerDatabase,
        service: TimeTrackerService,
        dataSource: TimeTrackerRepository
    ): TrackerServices {
        return TrackerServices(preferences, db, service, dataSource)
    }
}