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

import android.net.Uri;

import org.greenrobot.greendao.annotation.Convert;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.Generated;

/**
 * User entity.
 *
 * @author Moshe Waisberg.
 */
@Entity
public class User implements TikalEntity {

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

    /**
     * Unique username.
     */
    @NotNull
    private String username;
    /**
     * The e-mail address for communications.
     */
    private String email;
    /**
     * The display name, e.g. full name.
     */
    private String displayName;
    /**
     * The telephone number for communications.
     */
    private String telephone;
    /**
     * The photo URI.
     */
    @Convert(converter = UriConverter.class, columnType = String.class)
    private Uri photo;
    /**
     * The role.
     */
    @NotNull
    @Convert(converter = RoleConverter.class, columnType = Integer.class)
    private Role role;

    @Generated(hash = 122985499)
    public User(long primaryId, long id, int version,
            @NotNull EntityStatus entityStatus, @NotNull String username,
            String email, String displayName, String telephone, Uri photo,
            @NotNull Role role) {
        this.primaryId = primaryId;
        this.id = id;
        this.version = version;
        this.entityStatus = entityStatus;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.telephone = telephone;
        this.photo = photo;
        this.role = role;
    }

    @Generated(hash = 586692638)
    public User() {
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Uri getPhoto() {
        return photo;
    }

    public void setPhoto(Uri photo) {
        this.photo = photo;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
