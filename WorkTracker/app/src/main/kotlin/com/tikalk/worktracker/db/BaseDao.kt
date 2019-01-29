package com.tikalk.worktracker.db

import androidx.room.*
import com.tikalk.worktracker.model.Project

/**
 * Base entity DAO.
 */
@Dao
interface BaseDao<E> {

    /**
     * Insert an entity in the database. If the entity already exists, replace it.
     *
     * @param entity the entity to be inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: E)

    /**
     * Update an entity.
     *
     * @param entity the entity to be updated.
     * @return the number of entities updated. This should always be 1.
     */
    @Update
    fun update(entity: E): Int

    /**
     * Delete an entity.
     * @param entity the entity to be deleted.
     */
    @Delete
    fun delete(entity: E)
}