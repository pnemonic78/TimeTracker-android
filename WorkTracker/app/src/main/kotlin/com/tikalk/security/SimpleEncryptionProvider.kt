package com.tikalk.security

/**
 * Simple encryption provider implementation.
 * @author moshe on 2018/07/18.
 */
class SimpleEncryptionProvider : EncryptionProvider {
    override fun hash(value: String): String {
        return value
    }

    override fun encrypt(clear: String?): String {
        return clear ?: ""
    }

    override fun decrypt(cryptic: String): String {
        return cryptic
    }
}