package com.tikalk.worktracker.start

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.TimeListActivity

class SplashActivity : AppCompatActivity() {

    private val context: Context = this

    private lateinit var prefs: TimeTrackerPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            prefs = TimeTrackerPrefs(this)
            maybeShowList()
        }
    }

    private fun maybeShowList() {
        if (prefs.userCredentials.isEmpty() || prefs.basicCredentials.isEmpty()) {
            startActivity(Intent(context, LoginActivity::class.java))
        } else {
            startActivity(Intent(context, TimeListActivity::class.java))
            finish()
        }
    }
}
