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
import com.tikalk.worktracker.model.TikalEntity
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.model.time.split
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.time.TimeEditFragment.Companion.STATE_DATE
import com.tikalk.worktracker.time.TimeEditFragment.Companion.STATE_RECORD
import com.tikalk.worktracker.time.TimeEditFragment.Companion.STATE_RECORD_ID
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.progress.*
import kotlinx.android.synthetic.main.time_form.*
import retrofit2.Response
import timber.log.Timber

class TimeEditActivity : TimeFormActivity() {

    // UI references
    private var submitMenuItem: MenuItem? = null
    private lateinit var editFragment: TimeEditFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the form.
        setContentView(R.layout.activity_time_edit)

        editFragment = supportFragmentManager.findFragmentById(R.id.fragmentForm) as TimeEditFragment
        formFragment = editFragment

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

    private fun populateForm(record: TimeRecord) {
        editFragment.populateForm(record)
    }

    private fun bindRecord(record: TimeRecord) {
        editFragment.bindRecord(record)
    }

    private fun authenticate(immediate: Boolean = false) {
        editFragment.authenticate(immediate)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date.timeInMillis)

        bindRecord(record)
        outState.putLong(STATE_RECORD_ID, record.id)
        outState.putParcelable(STATE_RECORD, record)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date.timeInMillis = savedInstanceState.getLong(STATE_DATE)
        val recordParcel = savedInstanceState.getParcelable<TimeRecord>(STATE_RECORD)

        if (recordParcel != null) {
            record = recordParcel
            populateForm(record)
        } else {
            record.id = savedInstanceState.getLong(STATE_RECORD_ID)
        }
    }

    private fun submit() {
        val record = this.record

        if (!validateForm(record)) {
            return
        }
        bindRecord(record)

        if (record.id == TikalEntity.ID_NONE) {
            val splits = record.split()
            val size = splits.size
            val lastIndex = size - 1
            for (i in 0 until size) {
                submit(splits[i], i == 0, i == lastIndex)
            }
        } else {
            submit(record, true, true)
        }
    }

    private fun submit(record: TimeRecord, first: Boolean = true, last: Boolean = true) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        if (first) {
            showProgress(true)
            errorLabel.text = ""
        }

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(this, authToken)

        val submitter: Single<Response<String>> = if (record.id == TikalEntity.ID_NONE) {
            service.addTime(record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note)
        } else {
            service.editTime(record.id,
                record.project.id,
                record.task.id,
                formatSystemDate(record.start),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note)
        }
        submitter
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (last) {
                    showProgress(false)
                }

                if (isValidResponse(response)) {
                    val body = response.body()!!
                    val errorMessage = getResponseError(body)
                    if (errorMessage.isNullOrEmpty()) {
                        if (last) {
                            setResult(RESULT_OK)
                            finish()
                        }
                    } else {
                        errorLabel.text = errorMessage
                    }
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error saving record: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }

    private fun validateForm(record: TimeRecord): Boolean {
        return editFragment.validateForm(record)
    }

    override fun showProgress(show: Boolean) {
        editFragment.view?.showAnimated(show.not())
        progress.showAnimated(show)

        submitMenuItem?.isEnabled = !show
    }

    private fun deleteRecord() {
        if (record.id == TikalEntity.ID_NONE) {
            setResult(RESULT_OK)
            finish()
        } else {
            deleteRecord(record)
        }
    }

    private fun deleteRecord(record: TimeRecord) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(this, authToken)

        service.deleteTime(record.id)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ response ->
                if (isValidResponse(response)) {
                    showProgress(false)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    authenticate(true)
                }
            }, { err ->
                Timber.e(err, "Error deleting record: ${err.message}")
                showProgress(false)
            })
            .addTo(disposables)
    }
}
