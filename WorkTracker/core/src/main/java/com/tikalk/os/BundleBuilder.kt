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

package com.tikalk.os

import android.os.Bundle
import android.os.IBinder
import android.os.Parcelable
import android.os.PersistableBundle
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import java.io.Serializable

class BundleBuilder {

    private val bundle = Bundle()

    fun putAll(bundle: PersistableBundle): BundleBuilder {
        this.bundle.putAll(bundle)
        return this
    }

    fun putAll(bundle: Bundle): BundleBuilder {
        this.bundle.putAll(bundle)
        return this
    }

    fun putBinder(key: String?, value: IBinder?): BundleBuilder {
        bundle.putBinder(key, value)
        return this
    }

    fun putBoolean(key: String?, value: Boolean): BundleBuilder {
        bundle.putBoolean(key, value)
        return this
    }

    fun putBooleanArray(key: String?, value: BooleanArray?): BundleBuilder {
        bundle.putBooleanArray(key, value)
        return this
    }

    fun putBundle(key: String?, value: Bundle?): BundleBuilder {
        bundle.putBundle(key, value)
        return this
    }

    fun putByte(key: String?, value: Byte): BundleBuilder {
        bundle.putByte(key, value)
        return this
    }

    fun putByteArray(key: String?, value: ByteArray?): BundleBuilder {
        bundle.putByteArray(key, value)
        return this
    }

    fun putChar(key: String?, value: Char): BundleBuilder {
        bundle.putChar(key, value)
        return this
    }

    fun putCharArray(key: String?, value: CharArray?): BundleBuilder {
        bundle.putCharArray(key, value)
        return this
    }

    fun putCharSequence(key: String?, value: CharSequence?): BundleBuilder {
        bundle.putCharSequence(key, value)
        return this
    }

    fun putCharSequenceArray(key: String?, value: Array<CharSequence?>?): BundleBuilder {
        bundle.putCharSequenceArray(key, value)
        return this
    }

    fun putCharSequenceArrayList(
        key: String?,
        value: ArrayList<CharSequence?>?
    ): BundleBuilder {
        bundle.putCharSequenceArrayList(key, value)
        return this
    }

    fun putDouble(key: String?, value: Double): BundleBuilder {
        bundle.putDouble(key, value)
        return this
    }

    fun putDoubleArray(key: String?, value: DoubleArray?): BundleBuilder {
        bundle.putDoubleArray(key, value)
        return this
    }

    fun putFloat(key: String?, value: Float): BundleBuilder {
        bundle.putFloat(key, value)
        return this
    }

    fun putFloatArray(key: String?, value: FloatArray?): BundleBuilder {
        bundle.putFloatArray(key, value)
        return this
    }

    fun putInt(key: String?, value: Int): BundleBuilder {
        bundle.putInt(key, value)
        return this
    }

    fun putIntArray(key: String?, value: IntArray?): BundleBuilder {
        bundle.putIntArray(key, value)
        return this
    }

    fun putIntegerArrayList(key: String?, value: ArrayList<Int?>?): BundleBuilder {
        bundle.putIntegerArrayList(key, value)
        return this
    }

    fun putLong(key: String?, value: Long): BundleBuilder {
        bundle.putLong(key, value)
        return this
    }

    fun putLongArray(key: String?, value: LongArray?): BundleBuilder {
        bundle.putLongArray(key, value)
        return this
    }

    fun putParcelable(key: String?, value: Parcelable?): BundleBuilder {
        bundle.putParcelable(key, value)
        return this
    }

    fun putParcelableArray(key: String?, value: Array<Parcelable?>?): BundleBuilder {
        bundle.putParcelableArray(key, value)
        return this
    }

    fun putParcelableArrayList(
        key: String?,
        value: ArrayList<out Parcelable?>?
    ): BundleBuilder {
        bundle.putParcelableArrayList(key, value)
        return this
    }

    fun putSerializable(key: String?, value: Serializable?): BundleBuilder {
        bundle.putSerializable(key, value)
        return this
    }

    fun putShort(key: String?, value: Short): BundleBuilder {
        bundle.putShort(key, value)
        return this
    }

    fun putShortArray(key: String?, value: ShortArray?): BundleBuilder {
        bundle.putShortArray(key, value)
        return this
    }

    fun putSize(key: String?, value: Size?): BundleBuilder {
        bundle.putSize(key, value)
        return this
    }

    fun putSizeF(key: String?, value: SizeF?): BundleBuilder {
        bundle.putSizeF(key, value)
        return this
    }

    fun putSparseParcelableArray(
        key: String?,
        value: SparseArray<out Parcelable?>?
    ): BundleBuilder {
        bundle.putSparseParcelableArray(key, value)
        return this
    }

    fun putString(key: String?, value: String?): BundleBuilder {
        bundle.putString(key, value)
        return this
    }

    fun putStringArray(key: String?, value: Array<String?>?): BundleBuilder {
        bundle.putStringArray(key, value)
        return this
    }

    fun putStringArrayList(key: String?, value: ArrayList<String?>?): BundleBuilder {
        bundle.putStringArrayList(key, value)
        return this
    }

    fun setClassLoader(loader: ClassLoader?): BundleBuilder {
        bundle.classLoader = loader
        return this
    }

    fun build(): Bundle {
        return bundle
    }
}