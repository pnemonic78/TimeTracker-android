package com.tikalk.model

/**
 * A result that holds a value.
 */
sealed class TikalResult<T> {

    class Loading<T> : TikalResult<T>()

    data class Success<T>(val data: T?) : TikalResult<T>()

    data class Error<T>(val exception: Exception, val code: Int = 0) : TikalResult<T>() {
        val message: String? get() = exception.message

        constructor(message: String, code: Int = 0) : this(
            exception = Exception(message),
            code = code
        )

        constructor(other: Error<*>) : this(other.exception, other.code)
    }
}