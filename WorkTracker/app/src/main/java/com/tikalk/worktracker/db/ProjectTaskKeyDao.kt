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

import androidx.room.Dao
import androidx.room.Query

/**
 * DAO for joining Project and Task entities.
 */
@Dao
interface ProjectTaskKeyDao : BaseDao<ProjectTaskKey> {

    /**
     * Select all keys from the table.
     *
     * @return all keys.
     */
    @Query("SELECT * FROM project_task_key")
    suspend fun queryAll(): List<ProjectTaskKey>

    /**
     * Select a project's keys.
     */
    @Query("SELECT * FROM project_task_key WHERE project_id = :projectId")
    suspend fun queryAllByProject(projectId: Long): List<ProjectTaskKey>

    /**
     * Delete all keys.
     */
    @Query("DELETE FROM project_task_key")
    suspend fun deleteAll(): Int

    /**
     * Delete all keys for projects.
     */
    @Query("DELETE FROM project_task_key WHERE project_id IN (:projectIds)")
    suspend fun deleteProjects(projectIds: Collection<Long>): Int

    /**
     * Delete all keys for tasks.
     */
    @Query("DELETE FROM project_task_key WHERE task_id IN (:taskIds)")
    suspend fun deleteTasks(taskIds: Collection<Long>): Int
}