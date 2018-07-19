package com.tikalk.worktracker.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import com.tikalk.security.CipherHelper
import com.tikalk.security.DefaultCipherHelper

/**
 * Secured preferences that encrypt/decrypt the keys and values.
 * @author moshe on 2018/07/17.
 */
class SecurePreferences(context: Context, name: String, mode: Int) : SharedPreferences {

    private val delegate: SharedPreferences = context.getSharedPreferences(name, mode)
    private val cipher: CipherHelper = DefaultCipherHelper()

    override fun contains(key: String): Boolean {
        return delegate.contains(hashKey(key))
    }

    override fun edit(): SharedPreferences.Editor {
        return Editor()
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
        return cipher.hash(key)
    }

    private fun encrypt(clear: String?): String {
        return cipher.encrypt(clear)
    }

    private fun decrypt(cryptic: String): String {
        return cipher.decrypt(cryptic)
    }

    private fun decryptBoolean(cryptic: String?, defValue: Boolean): Boolean {
        return if (cryptic == null) defValue else decrypt(cryptic).toBoolean()
    }

    private fun decryptFloat(cryptic: String?, defValue: Float): Float {
        return if (cryptic == null) defValue else decrypt(cryptic).toFloat()
    }

    private fun decryptInt(cryptic: String?, defValue: Int): Int {
        return if (cryptic == null) defValue else decrypt(cryptic).toInt()
    }

    private fun decryptLong(cryptic: String?, defValue: Long): Long {
        return if (cryptic == null) defValue else decrypt(cryptic).toLong()
    }

    private fun decryptString(cryptic: String?, defValue: String?): String? {
        return if (cryptic == null) defValue else decrypt(cryptic)
    }

    private fun decryptStringSet(cryptics: Set<String>?, defValue: Set<String>?): Set<String>? {
        return if (cryptics == null) {
            defValue
        } else {
            val result = LinkedHashSet<String>()
            for (cryptic in cryptics) {
                result.add(decrypt(cryptic))
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

    private inner class Editor : SharedPreferences.Editor {

        private val delegate: SharedPreferences.Editor = this@SecurePreferences.delegate.edit()

        override fun apply() {
            delegate.apply()
        }

        override fun clear(): SharedPreferences.Editor {
            delegate.clear()
            return this
        }

        override fun commit(): Boolean {
            return delegate.commit()
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            delegate.putString(hashKey(key), encrypt(value.toString()))
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            delegate.putString(hashKey(key), encrypt(value.toString()))
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            delegate.putString(hashKey(key), encrypt(value.toString()))
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            delegate.putString(hashKey(key), encrypt(value.toString()))
            return this
        }

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            delegate.putString(hashKey(key), encrypt(value))
            return this
        }

        override fun putStringSet(key: String, value: Set<String>): SharedPreferences.Editor {
            val items = LinkedHashSet<String>()
            for (item in value) {
                items.add(encrypt(item))
            }
            delegate.putStringSet(hashKey(key), items)
            return this
        }

        override fun remove(key: String): SharedPreferences.Editor {
            delegate.remove(hashKey(key))
            return this
        }
    }
}