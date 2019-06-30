package com.tikalk.security

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            simpleCipherO()
        } else {
            simpleCipherM()
        }
        simpleCipherDigest()
    }

    private fun simpleCipherM() {
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

    private fun simpleCipherO() {
        val key = "key"
        val cipher: CipherHelper = SimpleCipherHelper(key, "salt")

        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", cipher.hash("abc"))

        var cryptic = cipher.encrypt("def", key)
        assertEquals("J5LFXPfcM0d86rludTdcOg==", cryptic)
        var clear = cipher.decrypt(cryptic, key)
        assertEquals("def", clear)

        cryptic = cipher.encrypt("ghi", key)
        assertEquals("p+/yeBQl8nP2Lkaxi7OyLg==", cryptic)
        clear = cipher.decrypt(cryptic, key)
        assertEquals("ghi", clear)
    }

    private fun simpleCipherDigest() {
        val key = "key"

        val digest1 = MessageDigest.getInstance("SHA-1")
        assertNotNull(digest1)
        val cipher1: CipherHelper = SimpleCipherHelper(key, "salt", digest1)
        assertNotNull(cipher1)
        assertEquals("qZk+NkcGgWq6PiVxeFDCbJzQ2J0=", cipher1.hash("abc"))

        val digest256 = MessageDigest.getInstance("SHA-256")
        assertNotNull(digest256)
        val cipher256: CipherHelper = SimpleCipherHelper(key, "salt", digest256)
        assertNotNull(cipher256)
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", cipher256.hash("abc"))
    }
}
