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
import com.tikalk.worktracker.preference.TimeSettingsFragment
import kotlinx.android.synthetic.main.progress.*

class TimeListActivity : InternetActivity() {

    private lateinit var mainFragment: TimeListFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up the form and list.
        setContentView(R.layout.activity_time_list)
        val mainFragmentExisting = supportFragmentManager.findFragmentByTag(TAG_MAIN_FRAGMENT) as TimeListFragment?
        if (mainFragmentExisting != null) {
            mainFragment = mainFragmentExisting
        } else {
            mainFragment = TimeListFragment()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, mainFragment, TAG_MAIN_FRAGMENT)
                .commit()
        }

        handleIntent(intent, savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_settings) {
            showSettings()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showProgress(show: Boolean) {
        progress.showAnimated(show)
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

    override fun onBackPressed() {
        val settingsFragment = supportFragmentManager.findFragmentByTag(TAG_SETTINGS_FRAGMENT)
        if (settingsFragment != null) {
            supportFragmentManager.popBackStack()
            return
        }
        if (mainFragment.onBackPressed()) {
            return
        }
        super.onBackPressed()
    }

    private fun showSettings() {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, TimeSettingsFragment(), TAG_SETTINGS_FRAGMENT)
            .addToBackStack(TAG_SETTINGS_FRAGMENT)
            .commit()
    }

    companion object {
        const val ACTION_STOP = TimeListFragment.ACTION_STOP

        private const val TAG_MAIN_FRAGMENT = "main_list"
        private const val TAG_SETTINGS_FRAGMENT = "settings"
    }
}