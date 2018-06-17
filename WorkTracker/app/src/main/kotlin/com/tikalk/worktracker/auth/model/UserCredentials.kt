package com.tikalk.worktracker.auth.model

/**
 * Credentials for user authentication.
 * @author moshe on 2018/05/13.
 */
data class UserCredentials(var login: String, var password: String) {
    fun isEmpty(): Boolean = login.isEmpty() || password.isEmpty()
}