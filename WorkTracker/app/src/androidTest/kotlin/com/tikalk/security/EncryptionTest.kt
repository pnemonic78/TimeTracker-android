package com.tikalk.security

import android.os.Build
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory

/**
 * Encryption test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
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
        simpleCipherM()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            simpleCipherO()
        }
    }

    private fun simpleCipherM() {
        val key = "key"
        val salt = "salt"
        val algorithm = "PBKDF2withHmacSHA1"
        val keyFactory = SecretKeyFactory.getInstance(algorithm)
        val digest = MessageDigest.getInstance("SHA-1")
        assertNotNull(digest)
        val cipher: CipherHelper = SimpleCipherHelper(key, salt, keyFactory, digest)

        assertEquals("qZk+NkcGgWq6PiVxeFDCbJzQ2J0=", cipher.hash("abc"))

        var text = "def"
        var cryptic = cipher.encrypt(text, key)
        assertEquals("5AVlK2kXvqW7AZ4L+Xay5Q==", cryptic)
        var clear = cipher.decrypt(cryptic, key)
        assertEquals(text, clear)

        text = "ghi"
        cryptic = cipher.encrypt(text, key)
        assertEquals("dhyU6qOE0jpMH6a3fhUb7A==", cryptic)
        clear = cipher.decrypt(cryptic, key)
        assertEquals(text, clear)

        text = "The quick brown fox jumped over the lazy dog."
        cryptic = cipher.encrypt(text, key)
        assertEquals("bWmOyyc3BXVXCaPhU67Mbn+tcb5+8X/gvfNAQ6kR4hLjR+Nniwsvvz5rZOIZAEMy", cryptic)
        clear = cipher.decrypt(cryptic, key)
        assertEquals(text, clear)
    }

    private fun simpleCipherO() {
        val key = "key"
        val salt = "salt"
        val algorithm = "PBKDF2withHmacSHA256"
        val keyFactory = SecretKeyFactory.getInstance(algorithm)
        val digest = MessageDigest.getInstance("SHA-256")
        assertNotNull(digest)
        val cipher: CipherHelper = SimpleCipherHelper(key, salt, keyFactory, digest)

        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", cipher.hash("abc"))

        var text = "def"
        var cryptic = cipher.encrypt(text, key)
        assertEquals("J5LFXPfcM0d86rludTdcOg==", cryptic)
        var clear = cipher.decrypt(cryptic, key)
        assertEquals(text, clear)

        text = "ghi"
        cryptic = cipher.encrypt(text, key)
        assertEquals("p+/yeBQl8nP2Lkaxi7OyLg==", cryptic)
        clear = cipher.decrypt(cryptic, key)
        assertEquals(text, clear)

        text = "The quick brown fox jumped over the lazy dog."
        cryptic = cipher.encrypt(text, key)
        assertEquals("kpDBVnNa6GSTWLPnceJfS0F+Kybv6kiqpOcseRCTM0Zclzho5VdS/9E84ctlx+KU", cryptic)
        clear = cipher.decrypt(cryptic, key)
        assertEquals(text, clear)
    }

    @Test
    fun simpleCipherDigest() {
        val key = "key"
        val salt = "salt"

        val digest1 = MessageDigest.getInstance("SHA-1")
        assertNotNull(digest1)
        val cipher1: CipherHelper = SimpleCipherHelper(key, salt, digest1)
        assertNotNull(cipher1)
        assertEquals("qZk+NkcGgWq6PiVxeFDCbJzQ2J0=", cipher1.hash("abc"))

        val digest256 = MessageDigest.getInstance("SHA-256")
        assertNotNull(digest256)
        val cipher256: CipherHelper = SimpleCipherHelper(key, salt, digest256)
        assertNotNull(cipher256)
        assertEquals("ungWv48Bz+pBQUDeXa4iI7ADYaOWF3qctBD/YfIAFa0=", cipher256.hash("abc"))
    }
}
