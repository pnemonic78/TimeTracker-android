package com.tikalk.security

import androidx.test.runner.AndroidJUnit4
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
    fun defaultProvider() {
        val provider: EncryptionProvider = DefaultEncryptionProvider()
        assertEquals("abc", provider.hash("abc"))
        assertEquals("def", provider.encrypt("def"))
        assertEquals("ghi", provider.decrypt("ghi"))
    }

    @Test
    fun simpleProvider() {
        val provider: EncryptionProvider = SimpleEncryptionProvider()
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", provider.hash("abc"))
        assertEquals("def", provider.encrypt("def"))
        assertEquals("ghi", provider.decrypt("ghi"))
    }
}
