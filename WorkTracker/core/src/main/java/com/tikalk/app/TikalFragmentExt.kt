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

package com.tikalk.app

import android.app.Dialog
import android.os.Handler
import android.os.Looper
import androidx.annotation.IdRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.tikalk.compose.UnitCallback

fun Fragment.runOnUiThread(action: Runnable) {
    val activity = this.activity
    if (activity != null) {
        activity.runOnUiThread(action)
    } else {
        val handler = Handler(Looper.getMainLooper())
        handler.post(action)
    }
}

fun Fragment.runOnUiThread(action: UnitCallback) {
    val activity = this.activity
    if (activity != null) {
        activity.runOnUiThread(action)
    } else {
        val handler = Handler(Looper.getMainLooper())
        handler.post(action)
    }
}

fun Fragment.topLevel(): Fragment {
    return parentFragment?.topLevel() ?: this
}

fun DialogFragment.isShowing(): Boolean {
    val d: Dialog? = dialog
    if (d != null) {
        return (d.isShowing) and !isRemoving
    }
    return isVisible
}

fun <F : Fragment> FragmentManager.findFragmentByClass(clazz: Class<F>): F? {
    @Suppress("UNCHECKED_CAST")
    return fragments.find { clazz.isAssignableFrom(it.javaClass) } as F?
}

fun <F : Fragment> Fragment.findParentFragment(clazz: Class<F>): F? {
    var parent = parentFragment
    while (parent != null) {
        val parentClass = parent.javaClass
        if (clazz.isAssignableFrom(parentClass)) {
            @Suppress("UNCHECKED_CAST")
            return parent as F?
        }
        parent = parent.parentFragment
    }
    return null
}

fun Fragment.isNavDestination(@IdRes resId: Int): Boolean {
    return findNavController().isDestination(resId)
}

fun NavController.isDestination(@IdRes resId: Int): Boolean {
    val destination = this.currentDestination ?: return false
    return (destination.id == resId)
}
