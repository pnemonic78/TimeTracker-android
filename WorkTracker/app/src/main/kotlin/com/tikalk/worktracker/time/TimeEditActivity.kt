package com.tikalk.worktracker.time

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers

class TimeEditActivity : AppCompatActivity() {

    companion object {
        private val TAG = "TimeEditActivity"

        private const val REQUEST_AUTHENTICATE = 1

        private const val STATE_DATE = "date"
    }

    private lateinit var prefs: TimeTrackerPrefs

    // UI references
    private lateinit var project: Spinner
    private lateinit var task: Spinner
    private lateinit var dateText: TextView
    private lateinit var startTimeText: TextView
    private lateinit var finishTimeText: TextView
    private lateinit var noteText: EditText

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var fetchTask: Disposable? = null
    private var date: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)
        setContentView(R.layout.activity_time_edit)

        project = findViewById(R.id.project)
        task = findViewById(R.id.task)
        dateText = findViewById(R.id.date)
        startTimeText = findViewById(R.id.start)
        finishTimeText = findViewById(R.id.finish)
        noteText = findViewById(R.id.note)

        dateText.setOnClickListener { pickDate() }
        startTimeText.setOnClickListener { pickStartTime() }
        finishTimeText.setOnClickListener { pickFinishTime() }

        var date: Long
        if (savedInstanceState == null) {
            date = System.currentTimeMillis()
        } else {
            date = savedInstanceState.getLong(STATE_DATE, System.currentTimeMillis())
        }
        fetchPage(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchTask?.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_cancel -> finish()
            R.id.menu_submit -> submit()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchPage(date: Long) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        //TODO showProgress(true)
        //TODO disable menu items

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        val dateFormatted = DateFormat.format(DATE_PATTERN, date).toString()
        fetchTask = service.timeEditor(dateFormatted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    //TODO showProgress(false)
                    //TODO enable menu items

                    if (response.isSuccessful && !TextUtils.isEmpty(response.body())) {
                        this.date = date
                        populatePage(response.body()!!)
                    } else {
                        authenticate()
                    }
                }, { err ->
                    Log.e(TAG, "Error fetching page: ${err.message}", err)
                    //TODO showProgress(false)
                    //TODO enable menu items
                })
    }

    private fun populatePage(body: String) {
        println("±!@ [$body]")
        val dateFormatted = DateFormat.format(DATE_PATTERN, date).toString()
        dateText.text = dateFormatted
    }

    private fun authenticate() {
        startActivityForResult(Intent(this, LoginActivity::class.java), REQUEST_AUTHENTICATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_AUTHENTICATE) {
            if (resultCode == Activity.RESULT_OK) {
                fetchPage(date)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(STATE_DATE, date)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        date = savedInstanceState.getLong(STATE_DATE)
    }

    private fun submit() {
        TODO("submit the form to time.php")
    }

    private fun pickDate() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun pickStartTime() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun pickFinishTime() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
