package com.tikalk.worktracker.auth.model

import okhttp3.Credentials

/**
 * Credentials for Basic realm authentication.
 * @author moshe on 2018/05/13.
 */
data class BasicCredentials(var realm: String, var username: String, var password: String) {
    fun authToken(): String {
        return Credentials.basic(username, password)
    }

    companion object {
        const val SCHEME = "Basic"
    }
}