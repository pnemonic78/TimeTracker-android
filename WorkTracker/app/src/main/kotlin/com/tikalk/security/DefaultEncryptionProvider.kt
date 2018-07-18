package com.tikalk.security

/**
 * Default encryption provider implementation that does nothing.
 * @author moshe on 2018/07/18.
 */
class DefaultEncryptionProvider : EncryptionProvider {
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