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
        val key = "key"
        val cipher: CipherHelper = SimpleCipherHelper(key, "salt")

        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", cipher.hash("abc"))

        var cryptic = cipher.encrypt("def", key)
        assertEquals("5AVlK2kXvqW7AZ4L+Xay5Q==", cryptic)
        var clear = cipher.decrypt(cryptic, key)
        assertEquals("def", clear)

        cryptic = cipher.encrypt("ghi", key)
        assertEquals("dhyU6qOE0jpMH6a3fhUb7A==", cryptic)
        clear = cipher.decrypt(cryptic, key)
        assertEquals("ghi", clear)
    }
}
