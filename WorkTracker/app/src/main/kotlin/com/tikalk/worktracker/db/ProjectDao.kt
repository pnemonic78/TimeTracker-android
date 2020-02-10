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
package com.tikalk.worktracker.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.tikalk.worktracker.model.Project
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Project entity DAO.
 */
@Dao
interface ProjectDao : BaseDao<Project> {

    /**
     * Select all projects from the table.
     *
     * @return all projects.
     */
    @Query("SELECT * FROM project")
    fun queryAll(): List<Project>

    /**
     * Select all projects from the table.
     *
     * @return all projects.
     */
    @Query("SELECT * FROM project")
    fun queryAllSingle(): Single<List<Project>>

    /**
     * Select all projects from the table.
     *
     * @return all projects with their tasks.
     */
    @Transaction
    @Query("SELECT * FROM project")
    fun queryAllWithTasks(): List<ProjectWithTasks>

    /**
     * Select all projects from the table.
     *
     * @return all projects with their tasks.
     */
    @Transaction
    @Query("SELECT * FROM project")
    fun queryAllWithTasksLive(): LiveData<List<ProjectWithTasks>>

    /**
     * Select all projects from the table.
     *
     * @return all projects with their tasks.
     */
    @Transaction
    @Query("SELECT * FROM project")
    fun queryAllWithTasksSingle(): Single<List<ProjectWithTasks>>

    /**
     * Select all projects from the table.
     *
     * @return all projects with their tasks.
     */
    @Transaction
    @Query("SELECT * FROM project")
    fun queryAllWithTasksObservable(): Observable<List<ProjectWithTasks>>

    /**
     * Select a project by its id.
     */
    @Query("SELECT * FROM project WHERE id = :projectId")
    fun queryById(projectId: Long): Maybe<Project>

    /**
     * Delete all projects.
     */
    @Query("DELETE FROM project")
    fun deleteAll(): Int
}