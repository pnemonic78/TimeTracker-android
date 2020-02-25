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
package com.tikalk.net

import android.net.Uri
import android.os.Parcel
import android.text.format.DateUtils
import okhttp3.internal.http.HttpDate
import java.net.HttpCookie
import java.util.*

fun createUriFromParcel(parcel: Parcel): Uri? {
    try {
        return Uri.CREATOR.createFromParcel(parcel)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
    }
    return null
}

fun HttpCookie.format(): String {
    val sb = StringBuilder()

    sb.append(name).append("=\"").append(value).append('"')
    if (path != null) {
        sb.append("; path=\"").append(path).append('"')
    }
    if (domain != null) {
        sb.append("; domain=\"").append(domain).append('"')
    }
    if (portlist != null) {
        sb.append("; port=\"").append(portlist).append('"')
    }
    if (hasExpired()) {
        sb.append("; max-age=\"").append(0).append('"')
    } else {
        sb.append("; max-age=\"").append(maxAge).append('"')
    }

    return sb.toString()
}