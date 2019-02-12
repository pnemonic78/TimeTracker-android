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
package com.tikalk.worktracker.time

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_time_dialog.*
import kotlinx.android.synthetic.main.activity_time_edit.*
import kotlinx.android.synthetic.main.progress.*
import java.util.*

class TimeDialogActivity : InternetActivity() {

    companion object {
        private const val REQUEST_AUTHENTICATE = 1

        private const val STATE_DATE = "date"

        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".DATE"
    }

    private val context: Context = this

    private lateinit var prefs: TimeTrackerPrefs

    private val disposables = CompositeDisposable()
    private var date = Calendar.getInstance()
    private var user = User("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the form.
        setContentView(R.layout.activity_time_dialog)

        user.username = prefs.userCredentials.login
        user.email = user.username

        action_ai.setOnClickListener { startDialog() }

        handleIntent(intent, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle? = null) {
        val now = date.timeInMillis
        val extras = intent.extras
        if (extras != null) {
            val dateExtra = extras.getLong(EXTRA_DATE, now)
            date.timeInMillis = savedInstanceState?.getLong(STATE_DATE, dateExtra) ?: dateExtra
        } else {
            date.timeInMillis = savedInstanceState?.getLong(STATE_DATE, now) ?: now
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        time_form.visibility = if (show) View.GONE else View.VISIBLE
        time_form.animate().setDuration(shortAnimTime).alpha(
            (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                time_form.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progress.visibility = if (show) View.VISIBLE else View.GONE
        progress.animate().setDuration(shortAnimTime).alpha(
            (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    private fun startDialog() {
        conversation.text = "Hello, World!"
    }
}
