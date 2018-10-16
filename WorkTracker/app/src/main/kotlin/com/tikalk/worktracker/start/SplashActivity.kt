package com.tikalk.worktracker.start

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R
import com.tikalk.worktracker.auth.LoginActivity
import com.tikalk.worktracker.preference.TimeTrackerPrefs
import com.tikalk.worktracker.time.TimeListActivity
import kotlinx.android.synthetic.main.activity_splash.*

class SplashActivity : AppCompatActivity() {

    private val context: Context = this

    private lateinit var rotate: ObjectAnimator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        rotate = ObjectAnimator.ofInt(logo, "imageLevel", 0, 10000)
        rotate.duration = DateUtils.SECOND_IN_MILLIS
        rotate.repeatCount = ValueAnimator.INFINITE
        rotate.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        rotate.cancel()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        if (hasFocus) {
            showList()
        }
    }

    private fun showList() {
        startActivity(Intent(context, TimeListActivity::class.java))
        finish()
    }
}
