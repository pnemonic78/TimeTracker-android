package com.tikalk.worktracker.lang

val Boolean?.isTrue: Boolean
    get() = (this != null) && (this == true)

val Boolean?.isFalse: Boolean
    get() = (this != null) && (this == false)

fun CharSequence?.orEmpty(): CharSequence = this ?: ""
