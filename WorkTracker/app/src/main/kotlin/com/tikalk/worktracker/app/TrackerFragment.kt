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

package com.tikalk.worktracker.app

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.tikalk.app.TikalFragment
import com.tikalk.app.runOnUiThread
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.db.TrackerDatabase
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import org.koin.android.ext.android.inject

abstract class TrackerFragment : TikalFragment {

    constructor() : super()

    constructor(args: Bundle) : super(args)

    protected val preferences by inject<TimeTrackerPrefs>()
    protected val db by inject<TrackerDatabase>()

    protected var caller: Fragment? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = this.arguments
        if (args != null) {
            if (args.containsKey(EXTRA_CALLER)) {
                caller = findCaller(args)
            }
        }
    }

    private fun findCaller(args: Bundle): Fragment? {
        var fragment: Fragment = this
        while (true) {
            val fm = fragment.fragmentManager ?: return null
            try {
                val caller = fm.getFragment(args, EXTRA_CALLER)
                if (caller != null) {
                    return caller
                }
            } catch (e: IllegalStateException) {
            }
            fragment = fragment.parentFragment ?: return null
        }
    }

    protected abstract fun authenticate(submit: Boolean = false)

    protected fun authenticateMain(submit: Boolean = false) {
        runOnUiThread {
            authenticate(submit)
        }
    }

    companion object {
        const val EXTRA_CALLER = "callerId"
        const val EXTRA_ACTION = BuildConfig.APPLICATION_ID + ".ACTION"

        const val ACTION_STOP = BuildConfig.APPLICATION_ID + ".action.STOP"
    }
}