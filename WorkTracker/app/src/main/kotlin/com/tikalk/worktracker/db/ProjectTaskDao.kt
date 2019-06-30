package com.tikalk.worktracker.db

import androidx.room.Dao
import androidx.room.Query
import com.tikalk.worktracker.model.ProjectTask
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * Project Task entity DAO.
 */
@Dao
interface ProjectTaskDao : BaseDao<ProjectTask> {

    /**
     * Select all tasks from the tasks table.
     *
     * @return all tasks.
     */
    @Query("SELECT * FROM project_task")
    fun queryAll(): Single<List<ProjectTask>>

    /**
     * Select a task by its id.
     */
    @Query("SELECT * FROM project_task WHERE id = :taskId")
    fun queryById(taskId: Long): Maybe<ProjectTask>

    /**
     * Delete all entities.
     */
    @Query("DELETE FROM project_task")
    fun deleteAll()
}