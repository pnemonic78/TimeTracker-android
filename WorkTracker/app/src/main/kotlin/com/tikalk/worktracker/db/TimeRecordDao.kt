/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
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

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.tikalk.worktracker.model.time.TimeTotals
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Single

/**
 * Time record entity DAO.
 */
@Dao
interface TimeRecordDao : BaseDao<TimeRecordEntity> {

    /**
     * Select all records from the table.
     *
     * @return all records.
     */
    @Transaction
    @Query("SELECT * FROM record ORDER BY start ASC")
    fun queryAll(): List<WholeTimeRecordEntity>

    /**
     * Select all records from the table.
     *
     * @return all records.
     */
    @Transaction
    @Query("SELECT * FROM record ORDER BY start ASC")
    fun queryAllLive(): LiveData<List<WholeTimeRecordEntity>>

    /**
     * Select all records from the table.
     *
     * @return all records.
     */
    @Transaction
    @Query("SELECT * FROM record ORDER BY start ASC")
    fun queryAllSingle(): Single<List<WholeTimeRecordEntity>>

    /**
     * Select a record by its id.
     */
    @Transaction
    @Query("SELECT * FROM record WHERE id = :recordId")
    fun queryById(recordId: Long): WholeTimeRecordEntity?

    /**
     * Select a record by its id.
     */
    @Transaction
    @Query("SELECT * FROM record WHERE id = :recordId")
    fun queryByIdMaybe(recordId: Long): Maybe<WholeTimeRecordEntity>

    /**
     * Select records by their ids.
     */
    @Transaction
    @Query("SELECT * FROM record WHERE id IN (:recordIds)")
    fun queryByIds(recordIds: LongArray): List<WholeTimeRecordEntity>

    /**
     * Select all records from the table by date.
     *
     * @return all records between the dates.
     */
    @Transaction
    @Query("SELECT * FROM record WHERE (start >= :start) AND (finish <= :finish) ORDER BY start ASC")
    fun queryByDate(start: Long, finish: Long): List<WholeTimeRecordEntity>

    /**
     * Select all records from the table by date.
     *
     * @return all records between the dates.
     */
    @Transaction
    @Query("SELECT * FROM record WHERE (start >= :start) AND (finish <= :finish) ORDER BY start ASC")
    fun queryByDateLive(start: Long, finish: Long): LiveData<List<WholeTimeRecordEntity>>

    /**
     * Select all records from the table by date.
     *
     * @return all records between the dates.
     */
    @Transaction
    @Query("SELECT * FROM record WHERE (start >= :start) AND (finish <= :finish) ORDER BY start ASC")
    fun queryByDateSingle(start: Long, finish: Long): Single<List<WholeTimeRecordEntity>>

    /**
     * Select the totals.
     *
     * @return totals between the dates.
     */
    @Query(
        """SELECT SUM(finish - start) AS daily, 0 AS weekly, 0 AS monthly, 0 AS remaining FROM record WHERE (start >= :startDay) AND (finish <= :finishDay)
        UNION ALL SELECT 0 AS daily, SUM(finish - start) AS weekly, 0 AS monthly, 0 AS remaining FROM record WHERE (start >= :startWeek) AND (finish <= :finishWeek)
        UNION ALL SELECT 0 AS daily, 0 AS weekly, SUM(finish - start) AS monthly, 0 AS remaining FROM record WHERE (start >= :startMonth) AND (finish <= :finishMonth)"""
    )
    fun queryTotals(
        startDay: Long,
        finishDay: Long,
        startWeek: Long,
        finishWeek: Long,
        startMonth: Long,
        finishMonth: Long
    ): List<TimeTotals>

    /**
     * Delete all records.
     */
    @Query("DELETE FROM record")
    fun deleteAll(): Int

    /**
     * Delete all records for projects.
     */
    @Query("DELETE FROM record WHERE project_id IN (:projectIds)")
    fun deleteProjects(projectIds: Collection<Long>): Int

    /**
     * Delete all records for tasks.
     */
    @Query("DELETE FROM record WHERE task_id IN (:taskIds)")
    fun deleteTasks(taskIds: Collection<Long>): Int
}
