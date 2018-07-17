package com.tikalk.worktracker.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import java.util.*

/**
 * Secured preferences that encrypt/decrypt the keys and values.
 * @author moshe on 2018/07/17.
 */
class SecurePreferences(context: Context, name: String, mode: Int) : SharedPreferences {

    private val delegate: SharedPreferences = context.getSharedPreferences(name, mode)

    override fun contains(key: String): Boolean {
        return delegate.contains(hashKey(key))
    }

    override fun edit(): SharedPreferences.Editor {
        return delegate.edit()
    }

    override fun getAll(): Map<String, *> {
        return delegate.all
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return decryptBoolean(getEncryptedString(key), defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return decryptFloat(getEncryptedString(key), defValue)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return decryptInt(getEncryptedString(key), defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return decryptLong(getEncryptedString(key), defValue)
    }

    override fun getString(key: String, defValue: String?): String? {
        return decryptString(getEncryptedString(key), defValue)
    }

    override fun getStringSet(key: String, defValue: Set<String>?): Set<String>? {
        return decryptStringSet(getEncryptedStringSet(key), defValue)
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        return delegate.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        return delegate.unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun getEncryptedString(key: String): String? {
        return delegate.getString(hashKey(key), null)
    }

    private fun getEncryptedStringSet(key: String): Set<String>? {
        return delegate.getStringSet(hashKey(key), null)
    }

    private fun hashKey(key: String): String {
        //TODO implement me!
        return key
    }

    private fun encrypt(clear: String): String {
        //TODO implement me!
        return clear
    }

    private fun decrypt(cipher: String): String {
        //TODO implement me!
        return cipher
    }

    private fun decryptBoolean(cipher: String?, defValue: Boolean): Boolean {
        return if (cipher == null) defValue else decrypt(cipher).toBoolean()
    }

    private fun decryptFloat(cipher: String?, defValue: Float): Float {
        return if (cipher == null) defValue else decrypt(cipher).toFloat()
    }

    private fun decryptInt(cipher: String?, defValue: Int): Int {
        return if (cipher == null) defValue else decrypt(cipher).toInt()
    }

    private fun decryptLong(cipher: String?, defValue: Long): Long {
        return if (cipher == null) defValue else decrypt(cipher).toLong()
    }

    private fun decryptString(cipher: String?, defValue: String?): String? {
        return if (cipher == null) defValue else decrypt(cipher)
    }

    private fun decryptStringSet(ciphers: Set<String>?, defValue: Set<String>?): Set<String>? {
        return if (ciphers == null) {
            defValue
        } else {
            val result = LinkedHashSet<String>()
            for (cipher in ciphers) {
                result.add(decrypt(cipher))
            }
            return result
        }
    }

    companion object {

        /**
         * Gets a [SecurePreferences] instance that points to the default file that is used by
         * the preference framework in the given context.
         *
         * @param context The context of the preferences whose values are wanted.
         * @return A [SecurePreferences] instance that can be used to retrieve and listen
         * to values of the preferences.
         */
        fun getDefaultSharedPreferences(context: Context): SecurePreferences {
            return getSharedPreferences(context, getDefaultSharedPreferencesName(context),
                    getDefaultSharedPreferencesMode())
        }

        fun getSharedPreferences(context: Context, name: String, mode: Int): SecurePreferences {
            return SecurePreferences(context, name, mode)
        }

        /**
         * Returns the name used for storing default shared preferences.
         *
         * @param context The context of the preferences whose values are wanted.
         * @see #getDefaultSharedPreferences(Context)
         * @see Context#getSharedPreferencesPath(String)
         */
        fun getDefaultSharedPreferencesName(context: Context): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PreferenceManager.getDefaultSharedPreferencesName(context)
            } else {
                context.packageName + "_preferences"
            }
        }

        fun getDefaultSharedPreferencesMode(): Int {
            return Context.MODE_PRIVATE
        }
    }
}