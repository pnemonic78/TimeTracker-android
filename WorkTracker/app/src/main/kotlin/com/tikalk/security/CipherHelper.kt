package com.tikalk.security

import java.nio.charset.StandardCharsets

/**
 * Encryption provider.
 * @author moshe on 2018/07/18.
 */
interface CipherHelper {
    fun hash(value: String): String

    fun encrypt(clear: ByteArray, key: String = ""): String
    fun encrypt(clear: String, key: String = ""): String {
        return encrypt(clear.toByteArray(StandardCharsets.UTF_8), key)
    }

    fun decrypt(cryptic: ByteArray, key: String = ""): String
    fun decrypt(cryptic: String, key: String = ""): String {
        return decrypt(cryptic.toByteArray(StandardCharsets.UTF_8), key)
    }
}