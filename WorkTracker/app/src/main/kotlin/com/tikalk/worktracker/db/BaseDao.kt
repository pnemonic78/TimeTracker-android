/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2017, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
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