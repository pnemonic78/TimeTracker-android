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
package com.tikalk.worktracker.model;

import io.realm.RealmObject;

/**
 * Tikal base entity.
 *
 * @author Moshe Waisberg.
 */
public abstract class TikalEntity extends RealmObject {

    private long id;
    private int version;
    private int entityStatus = EntityStatus.INSERTED.ordinal();

    /**
     * Get the remote server ID.
     *
     * @return the id.
     */
    public long getId() {
        return id;
    }

    /**
     * Set the remote server ID.
     *
     * @param id the id.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Get the entity version to resolve conflicts.
     *
     * @return the version.
     */
    public int getVersion() {
        return version;
    }

    /**
     * Set the entity version to resolve conflicts.
     *
     * @param version the version.
     */
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * Get the entity status.
     *
     * @return the status.
     */
    public EntityStatus getEntityStatus() {
        return EntityStatus.values()[entityStatus];
    }

    /**
     * Set the entity status.
     *
     * @param entityStatus the status.
     */
    public void setEntityStatus(EntityStatus entityStatus) {
        this.entityStatus = entityStatus.ordinal();
    }
}
