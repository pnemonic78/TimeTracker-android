package com.tikalk.worktracker.start

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.TimeEditActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var prefs: TimeTrackerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = TimeTrackerPrefs(this)
        setContentView(R.layout.activity_splash)
    }

    override fun onResume() {
        super.onResume()
        maybeShowEditor()
    }

    private fun maybeShowEditor() {
        if (prefs.userCredentials.isEmpty() || prefs.basicCredentials.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            startActivity(Intent(this, TimeEditActivity::class.java))
            finish()
        }
    }
}
