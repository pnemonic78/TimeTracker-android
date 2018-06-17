package com.tikalk.worktracker.time

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import com.tikalk.worktracker.R

class TimeEditActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_edit)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.time_edit, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
