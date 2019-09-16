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

package com.tikalk.graphics

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.android.synthetic.main.activity_time_list.view.*

fun drawableToBitmap(drawable: Drawable): Bitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap
    }
    val width = drawable.intrinsicWidth
    val height = drawable.intrinsicHeight;
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp);
    drawable.setBounds(0, 0, width, height);
    drawable.draw(canvas);
    return bmp
}

fun drawableToBitmap(res: Resources, id: Int): Bitmap {
    val drawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        res.getDrawable(id, null)
    else
        res.getDrawable(id)
    return drawableToBitmap(drawable)
}