package com.tikalk.security

/**
 * Default encryption provider implementation that does nothing.
 * @author moshe on 2018/07/18.
 */
class DefaultCipherHelper : CipherHelper {
    override fun hash(value: String): String {
        return value
    }

    override fun encrypt(clear: ByteArray, key: String): String {
        return String(clear)
    }

    override fun encrypt(clear: String, key: String): String {
        return clear
    }

    override fun decrypt(cryptic: ByteArray, key: String): String {
        return String(cryptic)
    }

    override fun decrypt(cryptic: String, key: String): String {
        return cryptic
    }
}