/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2022, Tikal Knowledge, Ltd.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * • Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * • Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * • Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.tikalk.worktracker.auth

import android.util.Patterns

class LoginValidator {

    fun validateUsername(value: String): Int {
        if (value.isEmpty()) return ERROR_REQUIRED
        if (value.length < 3) return ERROR_LENGTH
        if (!isLoginValid(value)) return ERROR_INVALID
        return ERROR_NONE
    }

    private fun isLoginValid(login: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(login).matches()
    }

    fun validatePassword(value: String): Int {
        if (value.isEmpty()) return ERROR_REQUIRED
        if (value.length < 3) return ERROR_LENGTH
        if (!isPasswordValid(value)) return ERROR_INVALID
        return ERROR_NONE
    }

    fun validatePassword(value: String, confirmValue: String): Int {
        var error = validateUsername(confirmValue)
        if (error == ERROR_NONE) {
            if (value != confirmValue) error = ERROR_CONFIRM
        }
        return error
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.trim().length > 4
    }

    fun validateEmail(value: String): Int {
        if (value.isEmpty()) return ERROR_REQUIRED
        if (value.length < 3) return ERROR_LENGTH
        if (!isEmailValid(value)) return ERROR_INVALID
        return ERROR_NONE
    }

    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    companion object {
        const val ERROR_NONE = 0
        const val ERROR_REQUIRED = 1
        const val ERROR_LENGTH = 2
        const val ERROR_INVALID = 3
        const val ERROR_CONFIRM = 4
    }
}