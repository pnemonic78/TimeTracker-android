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
package com.tikalk.worktracker.time

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.tikalk.view.showAnimated
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetActivity
import kotlinx.android.synthetic.main.progress.*
import timber.log.Timber

class TimeEditActivity : InternetActivity(),
    TimeEditFragment.OnEditRecordListener {

    private lateinit var editFragment: TimeEditFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the form.
        setContentView(R.layout.activity_time_edit)

        editFragment = supportFragmentManager.findFragmentById(R.id.fragmentForm) as TimeEditFragment
        editFragment.listener = this

        handleIntent(intent, savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle? = null) {
        if (savedInstanceState == null) {
            editFragment.arguments = intent.extras
            editFragment.run()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(RESULT_CANCELED)
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showProgress(show: Boolean) {
        progress.showAnimated(show)
    }

    override fun onRecordEditSubmitted(fragment: TimeEditFragment, record: TimeRecord, last: Boolean) {
        Timber.i("record submitted: ${record.id} / ${record.project} / ${record.task}")
        if (last) {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onRecordEditDeleted(fragment: TimeEditFragment, record: TimeRecord) {
        Timber.i("record deleted: ${record.id} / ${record.project} / ${record.task}")
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onRecordEditFavorited(fragment: TimeEditFragment, record: TimeRecord) {
        Timber.i("record favorited: ${record.project} / ${record.task}")
    }

    override fun onRecordEditFailure(fragment: TimeEditFragment, record: TimeRecord, reason: String) {
        Timber.e("record failure: $reason")
    }
}
