package com.tikalk.worktracker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext: Context = ApplicationProvider.getApplicationContext()
        assertEquals("com.tikalk.worktracker${if (BuildConfig.DEBUG) ".debug" else ""}", appContext.packageName)
    }
}
