package com.tikalk.worktracker.db

import androidx.room.Dao
import androidx.room.Query
import com.tikalk.worktracker.model.Project

/**
 * Project entity DAO.
 */
@Dao
interface ProjectDao : BaseDao<Project> {

    /**
     * Select all articles from the articles table.
     *
     * @return all articles.
     */
    @Query("SELECT * FROM project")
    fun getAll(): List<Project>

    /**
     * Select a project by its id.
     */
    @Query("SELECT * FROM project WHERE id = :projectId")
    fun getById(projectId: Long): Project
}