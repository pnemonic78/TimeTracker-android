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
    private lateinit var submitMenuItem: MenuItem

    /** Keep track of the task to ensure we can cancel it if requested. */
    private var fetchTask: Disposable? = null
    /** Keep track of the task to ensure we can cancel it if requested. */
    private var submitTask: Disposable? = null
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
        submitTask?.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_edit, menu)
        submitMenuItem = menu.findItem(R.id.menu_submit)
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
                        this.record = TimeRecord(user, project, task)
                        bindForm(record)
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

    /** Populate the record and then bind the form. */
    private fun populateRecord(body: String, date: Long) {
//        record.start = Date(date)
//        record.finish = record.start
        bindForm(record)
    }

    private fun bindForm(record: TimeRecord) {
        val context = this
        dateText.text = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE)
        startTimeText.text = if (record.start != null) DateUtils.formatDateTime(context, record.start!!.timeInMillis, DateUtils.FORMAT_SHOW_TIME) else ""
        finishTimeText.text = if (record.finish != null) DateUtils.formatDateTime(context, record.finish!!.timeInMillis, DateUtils.FORMAT_SHOW_TIME) else ""
        noteText.setText(record.note)
        validateForm()
    }

    private fun bindRecord(record: TimeRecord) {
        record.note = noteText.text.toString()
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
        val record = this.record
        bindRecord(record)

        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        //TODO showProgress(true)
        //TODO disable menu items

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        submitTask = service.addTime(record.project.id,
                record.task.id,
                formatSystemDate(date),
                formatSystemTime(record.start),
                formatSystemTime(record.finish),
                record.note)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    //TODO showProgress(false)
                    //TODO enable menu items

                    if (validResponse(response)) {
                        populateRecord(response.body()!!, date)
                    } else {
                        authenticate()
                    }
                }, { err ->
                    Log.e(TAG, "Error saving page: ${err.message}", err)
                    //TODO showProgress(false)
                    //TODO enable menu items
                })
    }

    private fun pickDate() {
        val context = this
        val cal = Calendar.getInstance()
        cal.timeInMillis = date
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val listener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            this@TimeEditActivity.date = cal.timeInMillis
            val start = record.start
            if (start != null) {
                start.set(Calendar.YEAR, year)
                start.set(Calendar.MONTH, month)
                start.set(Calendar.DAY_OF_MONTH, day)
            }
            val finish = record.finish
            if (finish != null) {
                finish.set(Calendar.YEAR, year)
                finish.set(Calendar.MONTH, month)
                finish.set(Calendar.DAY_OF_MONTH, day)
            }
            dateText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_DATE)
            validateForm()
        }
        DatePickerDialog(context, listener, year, month, day).show()
    }

    private fun pickStartTime() {
        val context = this
        val cal = getCalendar(record.start)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            record.start = cal
            startTimeText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
            validateForm()
        }
        TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context)).show()
    }

    private inline fun getCalendar(cal: Calendar?): Calendar {
        if (cal == null) {
            val calDate = Calendar.getInstance()
            calDate.timeInMillis = date
            return calDate
        }
        return cal
    }

    private fun pickFinishTime() {
        val context = this
        val cal = getCalendar(record.finish)
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val listener = TimePickerDialog.OnTimeSetListener { picker, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            record.finish = cal
            finishTimeText.text = DateUtils.formatDateTime(context, cal.timeInMillis, DateUtils.FORMAT_SHOW_TIME)
            validateForm()
        }
        TimePickerDialog(context, listener, hour, minute, DateFormat.is24HourFormat(context)).show()
    }

    private fun validateForm(): Boolean {
        var valid = true

        if (record.project.id <= 0) {
            valid = false
            //TODO mark the field as invalid, e.g. red background
        }
        if (record.task.id <= 0) {
            valid = false
            //TODO mark the field as invalid, e.g. red background
        }
        if ((record.start == null) || (record.finish == null) || (record.start!! >= record.finish!!)) {
            valid = false
            //TODO mark the field as invalid, e.g. red background
        }

        submitMenuItem.isEnabled = valid
        return valid
    }
}
