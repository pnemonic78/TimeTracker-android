package com.tikalk.security

import android.util.Base64
import android.util.Base64.NO_WRAP
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Simple encryption provider implementation.
 * @author moshe on 2018/07/18.
 */
class SimpleEncryptionProvider : EncryptionProvider {

    private val digest: MessageDigest = MessageDigest.getInstance("SHA-256")

    override fun hash(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        digest.update(bytes)
        return Base64.encodeToString(digest.digest(), NO_WRAP)
    }

    override fun encrypt(clear: String?): String {
        return clear ?: ""
    }

    override fun decrypt(cryptic: String): String {
        return cryptic
    }
}