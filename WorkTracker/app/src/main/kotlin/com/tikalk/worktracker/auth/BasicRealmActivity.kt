package com.tikalk.worktracker.auth

import android.Manifest.permission.READ_CONTACTS
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.LoaderManager.LoaderCallbacks
import android.content.CursorLoader
import android.content.Loader
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import java.util.*

/**
 * An authentication screen for Basic Realm via email/password.
 */
class BasicRealmActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {

    // UI references.
    private lateinit var realmView: TextView
    private lateinit var usernameView: AutoCompleteTextView
    private lateinit var passwordView: EditText
    private lateinit var progressView: View
    private lateinit var loginFormView: View
    private lateinit var authButton: Button

    private lateinit var prefs: TimeTrackerPrefs
    private var realmName = "(realm)"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        val credentials = prefs.basicCredentials
        realmName = credentials.realm
        var userName = credentials.username
        var password = credentials.password
        val args = intent.extras
        if (args.containsKey(EXTRA_REALM)) {
            realmName = args.getString(EXTRA_REALM)
        }
        if (args.containsKey(EXTRA_USER)) {
            userName = args.getString(EXTRA_USER)
            if (userName != credentials.username) {
                password = ""
            }
        }

        // Set up the login form.
        setContentView(R.layout.activity_basic_realm)

        realmView = findViewById(R.id.realm_title)
        realmView.text = getString(R.string.authentication_basic_realm, realmName)

        usernameView = findViewById(R.id.username)
        populateAutoComplete()
        usernameView.setText(userName)

        passwordView = findViewById(R.id.password)
        passwordView.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        passwordView.setText(password)

        authButton = findViewById(R.id.realm_auth_button)
        authButton.setOnClickListener { attemptLogin() }

        loginFormView = findViewById(R.id.auth_form)
        progressView = findViewById(R.id.progress)
    }

    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        loaderManager.initLoader(0, Bundle(), this)
    }

    private fun mayRequestContacts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(usernameView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok) { requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS) }
        } else {
            requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
        }
        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (!authButton.isEnabled) {
            return
        }

        // Reset errors.
        usernameView.error = null
        passwordView.error = null

        // Store values at the time of the login attempt.
        val username = usernameView.text.toString()
        val password = passwordView.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passwordView.error = getString(R.string.error_invalid_password)
            focusView = passwordView
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(username)) {
            usernameView.error = getString(R.string.error_field_required)
            focusView = usernameView
            cancel = true
        } else if (!isUsernameValid(username)) {
            usernameView.error = getString(R.string.error_invalid_email)
            focusView = usernameView
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView!!.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            authButton.isEnabled = false

            prefs.basicCredentials = BasicCredentials(realmName, username, password)
            setResult(Activity.RESULT_OK)
            showProgress(false)
            finish()
        }
    }

    private fun isUsernameValid(username: String): Boolean {
        return username.length > 1
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        loginFormView.visibility = if (show) View.GONE else View.VISIBLE
        loginFormView.animate().setDuration(shortAnimTime).alpha(
                (if (show) 0 else 1).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                loginFormView.visibility = if (show) View.GONE else View.VISIBLE
            }
        })

        progressView.visibility = if (show) View.VISIBLE else View.GONE
        progressView.animate().setDuration(shortAnimTime).alpha(
                (if (show) 1 else 0).toFloat()).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                progressView.visibility = if (show) View.VISIBLE else View.GONE
            }
        })
    }

    override fun onCreateLoader(i: Int, bundle: Bundle): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {
    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this@BasicRealmActivity,
                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        usernameView.setAdapter(adapter)
    }

    private interface ProfileQuery {
        companion object {
            val PROJECTION = arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS, ContactsContract.CommonDataKinds.Email.IS_PRIMARY)

            val ADDRESS = 0
            val IS_PRIMARY = 1
        }
    }

    companion object {
        const val HEADER_WWW_AUTHENTICATE = "WWW-Authenticate"

        const val EXTRA_REALM = "realm"
        const val EXTRA_USER = "user"

        /**
         * Id to identity READ_CONTACTS permission request.
         */
        private const val REQUEST_READ_CONTACTS = 0
    }
}

