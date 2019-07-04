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
class SimpleCipherHelper(privateKey: String, salt: String, private val digest: MessageDigest) : CipherHelper {

    constructor(privateKey: String, salt: String) : this(privateKey, salt, try {
        MessageDigest.getInstance("SHA-256")
    } catch (e: Throwable) {
        MessageDigest.getInstance("SHA-1")
    })

    private val secretKey: Key
    private val cipherEncrypt: Cipher
    private val cipherDecrypt: Cipher

    init {
        val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "PBKDF2withHmacSHA256" else "PBKDF2withHmacSHA1"
        val keyFactory = SecretKeyFactory.getInstance(algorithm)

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