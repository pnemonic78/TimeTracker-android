/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Tikal Knowledge, Ltd.
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
package com.tikalk.security

import android.os.Build
import android.util.Base64
import android.util.Base64.NO_WRAP
import java.nio.charset.StandardCharsets
import java.security.Key
import java.security.MessageDigest
import java.security.spec.AlgorithmParameterSpec
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import kotlin.math.min

/**
 * Simple password-based encryption cipher helper.
 * @author moshe on 2018/07/18.
 */
class SimpleCipherHelper(
    privateKey: String,
    salt: String,
    keyFactory: SecretKeyFactory,
    private val digest: MessageDigest
) : CipherHelper {

    constructor(privateKey: String, salt: String, keyFactory: SecretKeyFactory) : this(
        privateKey, salt, keyFactory, try {
            MessageDigest.getInstance("SHA-256")
        } catch (e: Throwable) {
            MessageDigest.getInstance("SHA-1")
        }
    )

    constructor(privateKey: String, salt: String) : this(
        privateKey, salt,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            SecretKeyFactory.getInstance("PBKDF2withHmacSHA256")
        else
            SecretKeyFactory.getInstance("PBKDF2withHmacSHA1")
    )

    constructor(privateKey: String, salt: String, digest: MessageDigest) : this(
        privateKey, salt,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            SecretKeyFactory.getInstance("PBKDF2withHmacSHA256")
        else
            SecretKeyFactory.getInstance("PBKDF2withHmacSHA1"),
        digest
    )

    private val secretKey: Key
    private val cipherEncrypt: Cipher
    private val cipherDecrypt: Cipher

    init {
        val privateKeyChars = privateKey.toCharArray()

        val saltBytes = salt.toByteArray(StandardCharsets.UTF_8)
        val keySpec: KeySpec = PBEKeySpec(privateKeyChars, saltBytes, 1000, 256)
        secretKey = keyFactory.generateSecret(keySpec)

        val cipherAlgorithm = "AES/CBC/PKCS5PADDING"

        cipherEncrypt = Cipher.getInstance(cipherAlgorithm)
        cipherDecrypt = Cipher.getInstance(cipherAlgorithm)
    }

    override fun hash(value: String): String {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        digest.update(bytes)
        return Base64.encodeToString(digest.digest(), NO_WRAP)
    }

    @Synchronized
    override fun encrypt(clear: ByteArray, key: String): String {
        val aps = toParameterSpec(key)
        cipherEncrypt.init(Cipher.ENCRYPT_MODE, secretKey, aps)
        val cryptic = cipherEncrypt.doFinal(clear)
        return Base64.encodeToString(cryptic, NO_WRAP)
    }

    @Synchronized
    override fun decrypt(cryptic: ByteArray, key: String): String {
        val base64 = Base64.decode(cryptic, NO_WRAP)
        val aps = toParameterSpec(key)
        cipherDecrypt.init(Cipher.DECRYPT_MODE, secretKey, aps)
        val clear = cipherDecrypt.doFinal(base64)
        return String(clear, StandardCharsets.UTF_8)
    }

    private fun toParameterSpec(key: String): AlgorithmParameterSpec {
        val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
        val iv = ByteArray(16)
        System.arraycopy(keyBytes, 0, iv, 0, min(16, keyBytes.size))
        return IvParameterSpec(iv)
    }
}