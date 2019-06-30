package com.tikalk.worktracker.db

import androidx.room.Dao
import androidx.room.Query
import com.tikalk.worktracker.model.ProjectTaskKey
import io.reactivex.Maybe
import io.reactivex.Single

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
    fun queryAll(): Single<List<ProjectTaskKey>>

    /**
     * Select a project's keys.
     */
    @Query("SELECT * FROM project_task_key WHERE project_id = :projectId")
    fun queryAllByProject(projectId: Long): Maybe<List<ProjectTaskKey>>

    /**
     * Delete all entities.
     */
    @Query("DELETE FROM project_task_key")
    fun deleteAll()
}