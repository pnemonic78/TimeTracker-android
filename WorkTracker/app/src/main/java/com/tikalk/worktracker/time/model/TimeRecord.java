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

import com.tikalk.worktracker.model.EntityStatus;
import com.tikalk.worktracker.model.EntityStatusConverter;
import com.tikalk.worktracker.model.Project;
import com.tikalk.worktracker.model.ProjectTask;
import com.tikalk.worktracker.model.TikalEntity;
import com.tikalk.worktracker.model.User;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToOne;

import java.util.Date;
import org.greenrobot.greendao.DaoException;
import com.tikalk.worktracker.model.DaoSession;
import com.tikalk.worktracker.model.ProjectTaskDao;
import com.tikalk.worktracker.model.ProjectDao;
import com.tikalk.worktracker.model.UserDao;

/**
 * Time record entity. Represents some work done for a project task.
 *
 * @author Moshe Waisberg.
 */
@Entity
public class TimeRecord implements TikalEntity {

    /**
     * Local ID (Android database).
     */
    @Id
    private long primaryId;
    /**
     * Remote ID (server).
     */
    private long id;
    /**
     * Entity version to resolve conflicts.
     */
    private int version;
    /**
     * The entity status.
     */
    @NotNull
    @Convert(converter = EntityStatusConverter.class, columnType = Integer.class)
    private EntityStatus entityStatus;

    @NotNull
    @ToOne
    private User user;
    @NotNull
    @ToOne
    private Project project;
    @NotNull
    @ToOne
    private ProjectTask task;
    private Date start;
    private Date finish;
    private String note;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1317876036)
    private transient TimeRecordDao myDao;
    @Generated(hash = 2122020312)
    private transient boolean user__refreshed;
    @Generated(hash = 873765639)
    private transient boolean project__refreshed;
    @Generated(hash = 524156212)
    private transient boolean task__refreshed;

    @Generated(hash = 1982997001)
    public TimeRecord(long primaryId, long id, int version,
            @NotNull EntityStatus entityStatus, Date start, Date finish,
            String note) {
        this.primaryId = primaryId;
        this.id = id;
        this.version = version;
        this.entityStatus = entityStatus;
        this.start = start;
        this.finish = finish;
        this.note = note;
    }

    @Generated(hash = 1155170562)
    public TimeRecord() {
    }

    @Override
    public long getPrimaryId() {
        return primaryId;
    }

    @Override
    public void setPrimaryId(long primaryId) {
        this.primaryId = primaryId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    @Override
    public EntityStatus getEntityStatus() {
        return entityStatus;
    }

    @Override
    public void setEntityStatus(EntityStatus entityStatus) {
        this.entityStatus = entityStatus;
    }

    public Date getStart() {
        return start;
    }

    public void setStart(Date start) {
        this.start = start;
    }

    public Date getFinish() {
        return finish;
    }

    public void setFinish(Date finish) {
        this.finish = finish;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 2090372739)
    public User getUser() {
        if (user != null || !user__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            UserDao targetDao = daoSession.getUserDao();
            targetDao.refresh(user);
            user__refreshed = true;
        }
        return user;
    }

    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 1689829570)
    public User peakUser() {
        return user;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 396089742)
    public void setUser(@NotNull User user) {
        if (user == null) {
            throw new DaoException(
                    "To-one property 'user' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.user = user;
            user__refreshed = true;
        }
    }

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 342785038)
    public Project getProject() {
        if (project != null || !project__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            ProjectDao targetDao = daoSession.getProjectDao();
            targetDao.refresh(project);
            project__refreshed = true;
        }
        return project;
    }

    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 915411846)
    public Project peakProject() {
        return project;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1659465096)
    public void setProject(@NotNull Project project) {
        if (project == null) {
            throw new DaoException(
                    "To-one property 'project' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.project = project;
            project__refreshed = true;
        }
    }

    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1670794099)
    public ProjectTask getTask() {
        if (task != null || !task__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            ProjectTaskDao targetDao = daoSession.getProjectTaskDao();
            targetDao.refresh(task);
            task__refreshed = true;
        }
        return task;
    }

    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 1305481601)
    public ProjectTask peakTask() {
        return task;
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 420278760)
    public void setTask(@NotNull ProjectTask task) {
        if (task == null) {
            throw new DaoException(
                    "To-one property 'task' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.task = task;
            task__refreshed = true;
        }
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }

    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }

    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 825609909)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getTimeRecordDao() : null;
    }
}
