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
import com.tikalk.worktracker.net.InternetActivity
import kotlinx.android.synthetic.main.progress.*

class TimeListActivity : InternetActivity() {

    private lateinit var mainFragment: TimeListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the form and list.
        setContentView(R.layout.activity_time_list)
        mainFragment = supportFragmentManager.findFragmentById(R.id.fragmentFormAndList) as TimeListFragment

        handleIntent(intent, savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_date -> {
                mainFragment.pickDate()
                return true
            }
            R.id.menu_favorite -> {
                mainFragment.markFavorite()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showProgress(show: Boolean) {
        mainFragment.view?.showAnimated(show.not())
        progress.showAnimated(show)

        //recordAdd.isEnabled = !show
    }

    private fun handleIntent(intent: Intent, savedInstanceState: Bundle? = null) {
        if (savedInstanceState == null) {
            mainFragment.arguments = intent.extras
            mainFragment.run()

            if (intent.action == ACTION_STOP) {
                intent.action = null
                mainFragment.stopTimer()
            }
        }
        intent.action = null
    }

    companion object {
        const val REQUEST_EDIT = TimeListFragment.REQUEST_EDIT
        const val REQUEST_STOPPED = TimeListFragment.REQUEST_STOPPED

        const val ACTION_STOP = TimeListFragment.ACTION_STOP

        const val EXTRA_PROJECT_ID = TimeListFragment.EXTRA_PROJECT_ID
        const val EXTRA_TASK_ID = TimeListFragment.EXTRA_TASK_ID
        const val EXTRA_START_TIME = TimeListFragment.EXTRA_START_TIME
        const val EXTRA_FINISH_TIME = TimeListFragment.EXTRA_FINISH_TIME
    }
}