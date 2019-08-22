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
