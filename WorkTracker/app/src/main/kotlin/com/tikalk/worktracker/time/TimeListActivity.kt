package com.tikalk.worktracker.time

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_time_list.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import retrofit2.Response
import java.util.*

class TimeListActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TimeListActivity"

        private const val REQUEST_AUTHENTICATE = 1
        private const val REQUEST_ADD = 2

        private const val STATE_DATE = "date"
    }

    // UI references
    private var datePickerDialog: DatePickerDialog? = null

    private lateinit var prefs: TimeTrackerPrefs

    /** Keep track of the task to ensure we can cancel it if requested. */
    private var fetchTask: Disposable? = null
    private var date: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the form.
        setContentView(R.layout.activity_time_list)

        fab_add.setOnClickListener { addTime() }

        var date: Long
        val now = System.currentTimeMillis()
        if (savedInstanceState == null) {
            date = now
        } else {
            date = savedInstanceState.getLong(STATE_DATE, now)
        }
        fetchPage(date)
    }

    override fun onDestroy() {
        super.onDestroy()
        fetchTask?.dispose()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_date -> pickDate()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchPage(date: Long) {
        // Show a progress spinner, and kick off a background task to
        // perform the user login attempt.
        showProgress(true)

        val authToken = prefs.basicCredentials.authToken()
        val service = TimeTrackerServiceFactory.createPlain(authToken)

        val dateFormatted = formatSystemDate(date)
        fetchTask = service.fetchTimes(dateFormatted)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    showProgress(false)

                    this.date = date
                    if (validResponse(response)) {
                        populateList(response.body()!!, date)
                    } else {
                        authenticate(true)
                    }
                }, { err ->
                    Log.e(TAG, "Error fetching page: ${err.message}", err)
                })
    }

    private fun validResponse(response: Response<String>): Boolean {
        val body = response.body()
        if (response.isSuccessful && (body != null)) {
            val networkResponse = response.raw().networkResponse()
            val priorResponse = response.raw().priorResponse()
            if ((networkResponse != null) && (priorResponse != null) && priorResponse.isRedirect) {
                val networkUrl = networkResponse.request().url()
                val priorUrl = priorResponse.request().url()
                return networkUrl == priorUrl
            }
            return true
        }
        return false
    }

    /** Populate the list. */
    private fun populateList(html: String, date: Long) {
        val context: Context = this
        date_input.text = DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE)

        val doc: Document = Jsoup.parse(html)

        //TODO bindList()
    }

    private fun bindList() {
        val context: Context = this
        //TODO implement me!
    }

    private fun authenticate(immediate: Boolean = false) {
        val context: Context = this
        showProgress(true)
        val intent = Intent(context, LoginActivity::class.java)
        intent.putExtra(LoginActivity.EXTRA_SUBMIT, immediate)
        startActivityForResult(intent, REQUEST_AUTHENTICATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_AUTHENTICATE -> if (resultCode == Activity.RESULT_OK) {
                // Fetch the list for the user.
                fetchPage(date)
            }
            REQUEST_ADD -> if (resultCode == Activity.RESULT_OK) {
                // Refresh the list with the newly added item.
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

    private fun pickDate() {
        if (datePickerDialog == null) {
            val context: Context = this
            val cal = Calendar.getInstance()
            cal.timeInMillis = date
            val listener = DatePickerDialog.OnDateSetListener { picker, year, month, day ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)
                val date = cal.timeInMillis
                fetchPage(date)
            }
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH)
            val day = cal.get(Calendar.DAY_OF_MONTH)
            datePickerDialog = DatePickerDialog(context, listener, year, month, day)
        }
        datePickerDialog!!.show()
    }

    /**
     * Shows the progress UI and hides the list.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        list.visibility = if (show) View.GONE else View.VISIBLE
        list.animate().setDuration(shortAnimTime).alpha(
                (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                list.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progress.visibility = if (show) View.VISIBLE else View.GONE
        progress.animate().setDuration(shortAnimTime).alpha(
                (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progress.visibility = if (show) View.VISIBLE else View.GONE
            }
        })

        fab_add.isEnabled = !show
    }

    private fun addTime() {
        val context: Context = this
        val intent = Intent(context, TimeEditActivity::class.java)
        //TODO add "date" extra
        startActivityForResult(intent, REQUEST_ADD)
    }
}
