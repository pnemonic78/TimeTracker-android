package com.tikalk.security

/**
 * Encryption provider.
 * @author moshe on 2018/07/18.
 */
interface CipherHelper {
    fun hash(value: String): String
    fun encrypt(clear: String?): String
    fun decrypt(cryptic: String): String
}