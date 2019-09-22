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

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.tikalk.view.showAnimated
import com.tikalk.worktracker.R
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.time.TimeFormFragment.Companion.STATE_RECORD
import com.tikalk.worktracker.time.TimeFormFragment.Companion.STATE_RECORD_ID
import kotlinx.android.synthetic.main.progress.*

class TimeEditActivity : InternetActivity() {

    private var submitMenuItem: MenuItem? = null
    private lateinit var editFragment: TimeEditFragment

    private var record
        get() = editFragment.record
        set(value) {
            editFragment.record = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the form.
        setContentView(R.layout.activity_time_edit)

        editFragment = supportFragmentManager.findFragmentById(R.id.fragmentForm) as TimeEditFragment

        handleIntent(intent, savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle? = null) {
        editFragment.handleIntent(intent, savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_edit, menu)
        submitMenuItem = menu.findItem(R.id.menu_submit)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                setResult(RESULT_CANCELED)
                onBackPressed()
                return true
            }
            R.id.menu_delete -> {
                deleteRecord()
                return true
            }
            R.id.menu_submit -> {
                submit()
                return true
            }
            R.id.menu_favorite -> {
                markFavorite()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun bindForm(record: TimeRecord) {
        editFragment.bindForm(record)
    }

    private fun bindRecord(record: TimeRecord) {
        editFragment.bindRecord(record)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        bindRecord(record)
        outState.putLong(STATE_RECORD_ID, record.id)
        outState.putParcelable(STATE_RECORD, record)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)

        if (recordParcel != null) {
            record = recordParcel
            bindForm(record)
        } else {
            record.id = savedInstanceState.getLong(STATE_RECORD_ID)
        }
    }

    private fun submit() {
        editFragment.submit()
    }

    override fun showProgress(show: Boolean) {
        editFragment.view?.showAnimated(show.not())
        progress.showAnimated(show)

        submitMenuItem?.isEnabled = !show
    }

    private fun deleteRecord() {
        editFragment.deleteRecord()
    }

    private fun markFavorite() {
        editFragment.markFavorite()
    }
}
