package com.tikalk.worktracker.db

import androidx.room.*
import com.tikalk.worktracker.model.ProjectTask

/**
 * Project Task entity DAO.
 */
@Dao
interface ProjectTaskDao {

    /**
     * Select all tasks from the tasks table.
     *
     * @return all tasks.
     */
    @Query("SELECT * FROM project_task")
    fun getTasks(): List<ProjectTask>

    /**
     * Select a task by its id.
     */
    @Query("SELECT * FROM project_task WHERE id = :taskId")
    fun getTaskById(taskId: String): ProjectTask

    /**
     * Insert a task in the database. If the task already exists, replace it.
     *
     * @param task the task to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTask(task: ProjectTask)

    /**
     * Update a task.
     *
     * @param task the task to be updated.
     * @return the number of tasks updated. This should always be 1.
     */
    @Update
    fun updateTask(task: ProjectTask): Int

    /**
     * Delete all tasks.
     */
    @Query("DELETE FROM project_task")
    fun deleteTasks()
}