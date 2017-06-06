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
package com.tikalk.worktracker.time.model;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.tikalk.worktracker.model.TikalDao;

import java.util.List;

import io.reactivex.Flowable;

/**
 * Report filter DAO.
 *
 * @author Moshe Waisberg.
 */
@Dao
public interface ReportFilterDao extends TikalDao<ReportFilterEntity> {
    @Query("SELECT * FROM reportFilter")
    Flowable<List<ReportFilterEntity>> queryAll();

    @Query("SELECT * FROM reportFilter WHERE userId = :userId")
    Flowable<List<ReportFilterEntity>> queryAll(long userId);

    @Query("SELECT * FROM reportFilter WHERE userId = :userId AND favorite IS NOT NULL AND favorite <> ''")
    Flowable<List<ReportFilterEntity>> queryFavorites(long userId);

    @Query("SELECT * FROM reportFilter where _id = :id LIMIT 1")
    Flowable<ReportFilterEntity> query(long id);

    @Insert
    void insert(ReportFilterEntity entity);

    @Insert
    void insertAll(ReportFilterEntity... entities);

    @Update
    void update(ReportFilterEntity entity);

    @Update
    void updateAll(ReportFilterEntity... entities);

    @Delete
    void delete(ReportFilterEntity entity);

    @Delete
    void deleteAll(ReportFilterEntity... entities);
}
