package com.tikalk.worktracker.net

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Authentication interceptor.
 * @author moshe on 2018/05/13.
 * @see https://futurestud.io/tutorials/android-basic-authentication-with-retrofit
 */
class AuthenticationInterceptor(private val authToken: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = original.newBuilder()
                .header("Authorization", authToken)
                .build()
        return chain.proceed(request)
    }
}