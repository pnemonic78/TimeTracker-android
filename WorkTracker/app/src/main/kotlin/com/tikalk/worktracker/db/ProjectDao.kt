package com.tikalk.worktracker.db

import androidx.room.*
import com.tikalk.worktracker.model.Project

/**
 * Project entity DAO.
 */
@Dao
interface ProjectDao {

    /**
     * Select all articles from the articles table.
     *
     * @return all articles.
     */
    @Query("SELECT * FROM project")
    fun getProjects(): List<Project>

    /**
     * Select a project by its id.
     */
    @Query("SELECT * FROM project WHERE id = :projectId")
    fun getProjectById(projectId: String): Project

    /**
     * Insert a project in the database. If the project already exists, replace it.
     *
     * @param project the project to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProject(project: Project)

    /**
     * Update a project.
     *
     * @param project the project to be updated.
     * @return the number of projects updated. This should always be 1.
     */
    @Update
    fun updateProject(project: Project): Int

    /**
     * Delete all projects.
     */
    @Query("DELETE FROM project")
    fun deleteProjects()
}