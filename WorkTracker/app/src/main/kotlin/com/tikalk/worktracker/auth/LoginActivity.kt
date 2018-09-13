package com.tikalk.worktracker.auth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.auth.model.UserCredentials
import com.tikalk.worktracker.net.TimeTrackerServiceFactory
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.formatSystemDate
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.Response

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"

        private const val REQUEST_AUTHENTICATE = 1

        const val EXTRA_EMAIL = "email"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }

    private lateinit var prefs: TimeTrackerPrefs

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var authTask: Disposable? = null

    // UI references.
    private lateinit var emailView: EditText
    private lateinit var passwordView: EditText
    private lateinit var progressView: View
    private lateinit var loginFormView: View
    private lateinit var emailSignInButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)

        // Set up the login form.
        setContentView(R.layout.activity_login)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        emailView = findViewById(R.id.email)
        emailView.setText(prefs.userCredentials.login)

        passwordView = findViewById(R.id.password)
        passwordView.setOnEditorActionListener(TextView.OnEditorActionListener { textView, id, keyEvent ->
            if (id == R.id.login || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        passwordView.setText(prefs.userCredentials.password)

        emailSignInButton = findViewById(R.id.email_sign_in_button)
        emailSignInButton.setOnClickListener { attemptLogin() }

        loginFormView = findViewById(R.id.login_form)
        progressView = findViewById(R.id.login_progress)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val extras = intent.extras ?: return

        if (extras.containsKey(EXTRA_EMAIL)) {
            emailView.setText(extras.getString(EXTRA_EMAIL))
        }
        if (extras.containsKey(EXTRA_PASSWORD)) {
            passwordView.setText(extras.getString(EXTRA_PASSWORD))
        }
        if (extras.containsKey(EXTRA_SUBMIT)) {
            if (extras.getBoolean(EXTRA_SUBMIT)) {
                attemptLogin()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        authTask?.dispose()
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if (!emailSignInButton.isEnabled) {
            return
        }

        // Reset errors.
        emailView.error = null
        passwordView.error = null

        // Store values at the time of the login attempt.
        val email = emailView.text.toString()
        val password = passwordView.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (password.isEmpty()) {
            passwordView.error = getString(R.string.error_field_required)
            focusView = passwordView
            cancel = true
        } else if (!isPasswordValid(password)) {
            passwordView.error = getString(R.string.error_invalid_password)
            focusView = passwordView
            cancel = true
        }

        // Check for a valid email address.
        if (email.isEmpty()) {
            emailView.error = getString(R.string.error_field_required)
            focusView = emailView
            cancel = true
        } else if (!isEmailValid(email)) {
            emailView.error = getString(R.string.error_invalid_email)
            focusView = emailView
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
            emailSignInButton.isEnabled = false

            prefs.userCredentials = UserCredentials(email, password)

            val authToken = prefs.basicCredentials.authToken()
            val service = TimeTrackerServiceFactory.createPlain(authToken)

            val today = formatSystemDate()
            authTask = service.login(email, password, today)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        showProgress(false)
                        emailSignInButton.isEnabled = true

                        val body = response.body()
                        if (response.isSuccessful && (body != null)) {
                            setResult(Activity.RESULT_OK)
                            finish()
                        } else {
                            authenticate(email, response.raw())
                        }
                    }, { err ->
                        Log.e(TAG, "Error signing in: ${err.message}", err)
                        showProgress(false)
                        emailSignInButton.isEnabled = true
                    })
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
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

    private fun authenticate(email: String, response: Response): Boolean {
        val challenges = response.challenges()
        for (challenge in challenges) {
            if (challenge.scheme() == BasicCredentials.SCHEME) {
                val realm = challenge.realm()
                val indexAt = email.indexOf('@')
                val username = if (indexAt < 0) email else email.substring(0, indexAt)
                val context: Context = this
                val intent = Intent(context, BasicRealmActivity::class.java)
                intent.putExtra(BasicRealmActivity.EXTRA_REALM, realm)
                intent.putExtra(BasicRealmActivity.EXTRA_USER, username)
                startActivityForResult(intent, REQUEST_AUTHENTICATE)
                return true
            }
        }
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }
}

