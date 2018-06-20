package com.tikalk.worktracker.time

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.model.Project
import com.tikalk.worktracker.model.ProjectTask
import com.tikalk.worktracker.model.User
import com.tikalk.worktracker.model.time.TimeRecord
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import retrofit2.Response
import java.util.*

class TimeEditActivity : AppCompatActivity() {

    companion object {
        private val TAG = "TimeEditActivity"

        private const val REQUEST_AUTHENTICATE = 1

        private const val STATE_DATE = "date"
    }

    private lateinit var prefs: TimeTrackerPrefs

    // UI references
    private lateinit var projectSpinner: Spinner
    private lateinit var taskSpinner: Spinner
    private lateinit var dateText: TextView
    private lateinit var startTimeText: TextView
    private lateinit var finishTimeText: TextView
    private lateinit var noteText: EditText

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var fetchTask: Disposable? = null
    private var date: Long = 0L
    private var user = User("")
    private var project = Project("")
    private var task = ProjectTask("")
    private var record = TimeRecord(user, project, task)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)
        setContentView(R.layout.activity_time_edit)

        user.username = prefs.userCredentials.login
        user.email = user.username

        projectSpinner = findViewById(R.id.project)
        taskSpinner = findViewById(R.id.task)
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

        val dateFormatted = formatSystemDate(date)
        fetchTask = service.fetchTimes(dateFormatted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    //TODO showProgress(false)
                    //TODO enable menu items

                    this.date = date
                    if (validResponse(response)) {
                        populateRecord(response.body()!!, date)
                    } else {
                        authenticate()
                    }
                }, { err ->
                    Log.e(TAG, "Error fetching page: ${err.message}", err)
                    //TODO showProgress(false)
                    //TODO enable menu items
                })
    }

    private fun validResponse(response: Response<String>): Boolean {
        val body = response.body()
        if (response.isSuccessful && (body != null)) {
            val networkResponse = response.raw().networkResponse()
            val priorResponse = response.raw().priorResponse()
            if ((networkResponse != null) && (priorResponse != null) && priorResponse.isRedirect) {
                //val redirectUrl = networkResponse.request().url()
                //val paths = redirectUrl.pathSegments()
                //return paths.last() != "login.php"
                return false
            }
            return true
        }
        return false
    }

    private fun populateRecord(body: String, date: Long) {
        //TODO populate the record and then bind the form.
        record.start = Date(date)
        record.finish = record.start
        bindForm(record)
    }

    private fun bindForm(record: TimeRecord) {
        val context = this
        dateText.text = DateUtils.formatDateTime(context, record.start!!.time, DateUtils.FORMAT_SHOW_DATE)
        startTimeText.text = DateUtils.formatDateTime(context, record.start!!.time, DateUtils.FORMAT_SHOW_TIME)
        finishTimeText.text = DateUtils.formatDateTime(context, record.finish!!.time, DateUtils.FORMAT_SHOW_TIME)
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
        val context = this
        val cal = Calendar.getInstance()
        cal.time = record.start
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val listener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            val date = cal.time
            record.start = date
            dateText.text = DateUtils.formatDateTime(context, date.time, DateUtils.FORMAT_SHOW_DATE)
        }
        DatePickerDialog(context, listener, year, month, day).show()
    }

    private fun pickStartTime() {
        val context = this
        val cal = Calendar.getInstance()
        cal.time = record.start
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            val date = cal.time
            record.start = date
            startTimeText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
        }
        TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context)).show()
    }

    private fun pickFinishTime() {
        val context = this
        val cal = Calendar.getInstance()
        cal.time = record.finish
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            val date = cal.time
            record.finish = date
            finishTimeText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
        }
        TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context)).show()
    }
}
