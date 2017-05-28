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

import android.arch.persistence.room.TypeConverter;
import android.net.Uri;
import android.text.TextUtils;

import com.tikalk.worktracker.time.model.TaskRecordStatus;

import java.sql.Date;

import static android.arch.persistence.room.util.StringUtil.EMPTY_STRING_ARRAY;

/**
 * Tikal data type converter.
 *
 * @author Moshe Waisberg.
 */
public class TikalConverter {
    @TypeConverter
    public static Date timestampToDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public static Uri pathToUri(String value) {
        return value == null ? null : Uri.parse(value);
    }

    @TypeConverter
    public static String uriToPath(Uri uri) {
        return uri == null ? null : uri.toString();
    }

    @TypeConverter
    public static ReportTimePeriod toReportTimePeriod(int value) {
        return value == -1 ? null : ReportTimePeriod.values()[value];
    }

    @TypeConverter
    public static int fromReportTimePeriod(ReportTimePeriod period) {
        return period == null ? -1 : period.ordinal();
    }

    @TypeConverter
    public static TaskRecordStatus toTaskRecordStatus(int value) {
        return value == -1 ? null : TaskRecordStatus.values()[value];
    }

    @TypeConverter
    public static int fromTaskRecordStatus(TaskRecordStatus status) {
        return status == null ? -1 : status.ordinal();
    }

    @TypeConverter
    public static String[] toStringArray(String value) {
        return value == null ? EMPTY_STRING_ARRAY : TextUtils.split(value, "\n");
    }

    @TypeConverter
    public static String fromStringArray(String[] roles) {
        return roles == null ? null : TextUtils.join("\n", roles);
    }
}
