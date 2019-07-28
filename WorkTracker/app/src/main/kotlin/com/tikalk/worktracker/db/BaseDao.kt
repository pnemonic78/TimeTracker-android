package com.tikalk.worktracker.db

import androidx.room.*

/**
 * Base entity DAO.
 */
@Dao
interface BaseDao<E> {

    /**
     * Insert an entity in the database. If the entity already exists, replace it.
     *
     * @param entity the entity to be inserted.
     * @return the entity id.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: E): Long

    /**
     * Insert entities in the database. If an entity already exists, replace it.
     *
     * @param entities the entities to be inserted.
     * @return the entity ids.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entities: Array<E>): LongArray

    /**
     * Insert entities in the database. If an entity already exists, replace it.
     *
     * @param entities the entities to be inserted.
     * @return the entity ids.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entities: Collection<E>): LongArray

    /**
     * Update an entity.
     *
     * @param entity the entity to be updated.
     * @return the number of entities updated. This should always be 1.
     */
    @Update
    fun update(entity: E): Int

    /**
     * Update entities.
     *
     * @param entities the entities to be updated.
     * @return the number of entities updated.
     */
    @Update
    fun update(entities: Array<E>): Int

    /**
     * Update entities.
     *
     * @param entities the entities to be updated.
     * @return the number of entities updated.
     */
    @Update
    fun update(entities: Collection<E>): Int

    /**
     * Delete an entity.
     * @param entity the entity to be deleted.
     * @return the number of entities deleted.
     */
    @Delete
    fun delete(entity: E): Int

    /**
     * Delete entities.
     * @param entities the entities to be deleted.
     * @return the number of entities deleted.
     */
    @Delete
    fun delete(entities: Array<E>): Int

    /**
     * Delete entities.
     * @param entities the entities to be deleted.
     * @return the number of entities deleted.
     */
    @Delete
    fun delete(entities: Collection<E>): Int
}