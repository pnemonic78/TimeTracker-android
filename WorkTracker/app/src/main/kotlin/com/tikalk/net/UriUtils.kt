package com.tikalk.net

import android.net.Uri
import android.os.Parcel

fun createUriFromParcel(parcel: Parcel): Uri? {
    try {
        return Uri.CREATOR.createFromParcel(parcel)
    } catch (e: IllegalArgumentException) {
        e.printStackTrace()
    }
    return null
}
