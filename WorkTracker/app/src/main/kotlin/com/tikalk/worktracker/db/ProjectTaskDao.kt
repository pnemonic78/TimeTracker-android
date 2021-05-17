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
import com.tikalk.worktracker.model.ProjectTask
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

/**
 * Project Task entity DAO.
 */
@Dao
interface ProjectTaskDao : BaseDao<ProjectTask> {

    /**
     * Select all tasks from the table.
     *
     * @return all tasks.
     */
    @Query("SELECT * FROM project_task")
    fun queryAll(): List<ProjectTask>

    /**
     * Select all tasks from the table.
     *
     * @return all tasks.
     */
    @Query("SELECT * FROM project_task")
    fun queryAllSingle(): Single<List<ProjectTask>>

    /**
     * Select all tasks from the table.
     *
     * @return all tasks.
     */
    @Query("SELECT * FROM project_task")
    fun queryAllLive(): LiveData<List<ProjectTask>>

    /**
     * Select a task by its id.
     */
    @Query("SELECT * FROM project_task WHERE id = :taskId")
    fun queryById(taskId: Long): Maybe<ProjectTask>

    /**
     * Delete all tasks.
     */
    @Query("DELETE FROM project_task")
    fun deleteAll(): Int
}