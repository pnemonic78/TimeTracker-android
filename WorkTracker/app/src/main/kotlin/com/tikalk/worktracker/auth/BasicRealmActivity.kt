package com.tikalk.worktracker.auth

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.model.BasicCredentials
import com.tikalk.worktracker.net.InternetActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs

/**
 * An authentication screen for Basic Realm via email/password.
 */
class BasicRealmActivity : InternetActivity() {

    companion object {
        const val EXTRA_REALM = "realm"
        const val EXTRA_USER = "user"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SUBMIT = "submit"
    }

    // UI references.
    private lateinit var realmView: TextView
    private lateinit var usernameView: EditText
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
        val userName = credentials.username
        val password = credentials.password

        // Set up the login form.
        setContentView(R.layout.activity_basic_realm)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        realmView = findViewById(R.id.realm_title)
        realmView.text = getString(R.string.authentication_basic_realm, realmName)

        usernameView = findViewById(R.id.username)
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

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        handleIntent(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.authenticate, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.menu_authenticate -> attemptLogin()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleIntent(intent: Intent) {
        val extras = intent.extras ?: return

        if (extras.containsKey(EXTRA_REALM)) {
            realmName = extras.getString(EXTRA_REALM) ?: "?"
            realmView.text = getString(R.string.authentication_basic_realm, realmName)
        }
        if (extras.containsKey(EXTRA_USER)) {
            usernameView.setText(extras.getString(EXTRA_USER))
            passwordView.text = null
        }
        if (extras.containsKey(EXTRA_PASSWORD)) {
            passwordView.setText(extras.getString(EXTRA_PASSWORD))
        }
        if (extras.containsKey(EXTRA_SUBMIT) && extras.getBoolean(EXTRA_SUBMIT)) {
            attemptLogin()
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

        // Check for a valid name.
        if (username.isEmpty()) {
            usernameView.error = getString(R.string.error_field_required)
            focusView = usernameView
            cancel = true
        } else if (!isUsernameValid(username)) {
            usernameView.error = getString(R.string.error_invalid_email)
            focusView = usernameView
            cancel = true
        }

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
            showProgress(false)
            setResult(RESULT_OK)
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
}

