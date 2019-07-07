package com.tikalk.worktracker.db

import androidx.room.*
import io.reactivex.Single

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
    fun insert(entity: E): Single<Long>

    /**
     * Insert entities in the database. If an entity already exists, replace it.
     *
     * @param entities the entities to be inserted.
     * @return the entity ids.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entities: Array<E>): Single<LongArray>

    /**
     * Insert entities in the database. If an entity already exists, replace it.
     *
     * @param entities the entities to be inserted.
     * @return the entity ids.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entities: Collection<E>): Single<LongArray>

    /**
     * Update an entity.
     *
     * @param entity the entity to be updated.
     * @return the number of entities updated. This should always be 1.
     */
    @Update
    fun update(entity: E): Single<Int>

    /**
     * Update entities.
     *
     * @param entities the entities to be updated.
     * @return the number of entities updated.
     */
    @Update
    fun update(entities: Array<E>): Single<Int>

    /**
     * Update entities.
     *
     * @param entities the entities to be updated.
     * @return the number of entities updated.
     */
    @Update
    fun update(entities: Collection<E>): Single<Int>

    /**
     * Delete an entity.
     * @param entity the entity to be deleted.
     * @return the number of entities deleted.
     */
    @Delete
    fun delete(entity: E): Single<Int>

    /**
     * Delete entities.
     * @param entities the entities to be deleted.
     * @return the number of entities deleted.
     */
    @Delete
    fun delete(entities: Array<E>): Single<Int>

    /**
     * Delete entities.
     * @param entities the entities to be deleted.
     * @return the number of entities deleted.
     */
    @Delete
    fun delete(entities: Collection<E>): Single<Int>
}