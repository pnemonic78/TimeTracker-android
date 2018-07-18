package com.tikalk.security

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Encryption test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class EncryptionTest {
    @Test
    fun simple() {
        // Context of the app under test.
        val context = InstrumentationRegistry.getContext()
        Assert.assertNotNull(context)

        val provider : EncryptionProvider = SimpleEncryptionProvider()
        assertEquals("abc", provider.hash("abc"))
        assertEquals("def", provider.encrypt("def"))
        assertEquals("ghi", provider.decrypt("ghi"))
    }
}
