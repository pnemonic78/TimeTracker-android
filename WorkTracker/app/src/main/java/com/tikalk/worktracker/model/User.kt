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
package com.tikalk.worktracker.model

import android.net.Uri
import android.os.Parcel
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.tikalk.net.createUriFromParcel

/**
 * User entity.
 *
 * @author Moshe Waisberg.
 */
@Entity(tableName = "user")
@TypeConverters(UserConverters::class)
data class User(
    /**
     * Unique username.
     */
    @ColumnInfo(name = "username")
    var username: String,
    /**
     * The e-mail address for communications.
     */
    @ColumnInfo(name = "email")
    var email: String? = null,
    /**
     * The display name, e.g. full name.
     */
    @ColumnInfo(name = "displayName")
    var displayName: String? = null,
    /**
     * The telephone number for communications.
     */
    @ColumnInfo(name = "telephone")
    var telephone: String? = null,
    /**
     * The photograph URI.
     */
    @ColumnInfo(name = "photograph")
    var photograph: Uri? = null,
    /**
     * The roles.
     */
    @ColumnInfo(name = "roles")
    var roles: List<String>? = null,
    /**
     * Are there any uncompleted entries?
     */
    @ColumnInfo(name = "uncompletedEntry")
    var isUncompletedEntry: Boolean = false
) : TikalEntity() {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        createUriFromParcel(parcel),
        parcel.createStringArrayList()
    )

    fun isEmpty(): Boolean {
        return username.isEmpty()
    }

    companion object {
        val EMPTY = User("")
    }
}

fun User.set(that: User) {
    this.username = that.username
    this.email = that.email
    this.displayName = that.displayName
    this.telephone = that.telephone
    this.photograph = that.photograph
    this.roles = that.roles
    this.isUncompletedEntry = that.isUncompletedEntry
}

class UserConverters : Converters() {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(", ")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(", ")
    }
}
