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
    fun defaultCipher() {
        val cipher: CipherHelper = DefaultCipherHelper()
        assertEquals("abc", cipher.hash("abc"))
        assertEquals("def", cipher.encrypt("def"))
        assertEquals("ghi", cipher.decrypt("ghi"))
    }

    @Test
    fun simpleCipher() {
        val cipher: CipherHelper = SimpleCipherHelper()
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", cipher.hash("abc"))
        assertEquals("def", cipher.encrypt("def"))
        assertEquals("ghi", cipher.decrypt("ghi"))
    }
}
