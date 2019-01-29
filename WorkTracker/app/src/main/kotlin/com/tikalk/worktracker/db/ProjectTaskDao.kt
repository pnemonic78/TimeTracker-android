package com.tikalk.worktracker.db

import androidx.room.Dao
import androidx.room.Query
import com.tikalk.worktracker.model.ProjectTask

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
    fun getAll(): List<ProjectTask>

    /**
     * Select a task by its id.
     */
    @Query("SELECT * FROM project_task WHERE id = :taskId")
    fun getById(taskId: Long): ProjectTask
}