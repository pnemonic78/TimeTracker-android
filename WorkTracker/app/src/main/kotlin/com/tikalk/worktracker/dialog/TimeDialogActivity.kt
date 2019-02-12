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
package com.tikalk.worktracker.dialog

import ai.api.AIConfiguration.SupportedLanguages
import ai.api.RequestExtras
import ai.api.android.AIConfiguration
import ai.api.android.AIConfiguration.RecognitionEngine
import ai.api.model.AIContext
import ai.api.model.AIError
import ai.api.model.AIResponse
import ai.api.model.Entity
import ai.api.ui.AIButton
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.UtteranceProgressListener
import android.text.format.DateUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.tikalk.worktracker.BuildConfig
import com.tikalk.worktracker.R
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_time_dialog.*
import timber.log.Timber
import java.util.*
import kotlin.collections.HashMap

class TimeDialogActivity : InternetActivity(), AIButton.AIButtonListener {

    companion object {
        private const val REQUEST_PERMISSIONS = 33

        private const val STATE_DATE = "date"

        const val EXTRA_DATE = BuildConfig.APPLICATION_ID + ".DATE"

        private const val AGENT_ACCESS_TOKEN = "4db8e909a1c649baa40f12f487eea2e5"
    }

    private lateinit var prefs: TimeTrackerPrefs

    private val disposables = CompositeDisposable()
    private var date = Calendar.getInstance()
    private val adapter = TimeDialogAdapter()
    private var response: AIResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the form.
        setContentView(R.layout.activity_time_dialog)

        val config = AIConfiguration(AGENT_ACCESS_TOKEN,
            SupportedLanguages.English,
            RecognitionEngine.System)
        action_ai.initialize(config)
        action_ai.setResultsListener(this)
        TTS.init(applicationContext)

        conversation.adapter = adapter

        checkAudioRecordPermission()

        handleIntent(intent, savedInstanceState)
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.dispose()
    }

    override fun onPause() {
        super.onPause()

        // use this method to disconnect from speech recognition service
        // Not destroying the SpeechRecognition object in onPause method would block other apps from using SpeechRecognition service
        action_ai.pause()
    }

    override fun onResume() {
        super.onResume()

        // use this method to re-init connection to recognition service
        action_ai.resume()
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

    override fun onCancelled() {
        runOnUiThread {
            Timber.w("onCancelled")
        }
    }

    override fun onError(error: AIError) {
        runOnUiThread {
            Timber.e("onError %s", error)
            adapter.add(error)
        }
    }

    override fun onResult(response: AIResponse) {
        Timber.d("onResult")
        this.response = response

        runOnUiThread {
            Timber.i("Received success response")
            adapter.add(response)
            conversation.scrollToPosition(adapter.itemCount - 1)

            // this is example how to get different parts of result object
            val status = response.status
            Timber.i("Status code: %s", status.code)
            Timber.i("Status type: %s", status.errorType)

            val result = response.result
            Timber.i("Resolved query: %s", result.resolvedQuery)

            Timber.i("Action: %s", result.action)
            val speech = result.fulfillment.speech
            Timber.i("Speech: $speech")
            TTS.speak(speech, utteranceProgressListener)

            val metadata = result.metadata
            if (metadata != null) {
                Timber.i("Intent id: %s", metadata.intentId)
                Timber.i("Intent name: %s", metadata.intentName)
            }

            val params = result.parameters
            if (params != null && !params.isEmpty()) {
                Timber.i("Parameters: ")
                for (entry in params.entries) {
                    Timber.i(String.format("%s: %s", entry.key, entry.value.toString()))
                }
            }

            // FIXME utteranceProgressListener not called by TTS.
            action_ai.postDelayed({ utteranceProgressListener.onDone("") }, DateUtils.SECOND_IN_MILLIS / 2)
        }
    }

    private fun checkAudioRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_PERMISSIONS)
            }
        }
    }

    private val utteranceProgressListener: UtteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            Timber.d("onStart utteranceId=%s", utteranceId)
        }

        override fun onDone(utteranceId: String) {
            Timber.d("onDone utteranceId=%s", utteranceId)
            listenAgain()
        }

        override fun onError(utteranceId: String) {
            Timber.d("onError utteranceId=%s", utteranceId)
            listenAgain()
        }
    }

    private fun listenAgain() {
        Timber.d("listenAgain")
        val response = this.response ?: return
        val result = response.result
        val contexts = result.contexts.map { out ->
            val ctx = AIContext(out.name)
            ctx.lifespan = out.lifespan

            val m = HashMap<String, String>()
            for (key in out.parameters.keys) {
                m[key] = out.parameters[key].toString()
            }
            ctx.parameters = m

            ctx
        }
        val entities: List<Entity> = emptyList()
        val requestExtras = RequestExtras(contexts, entities)
        action_ai.startListening(requestExtras)
    }
}
