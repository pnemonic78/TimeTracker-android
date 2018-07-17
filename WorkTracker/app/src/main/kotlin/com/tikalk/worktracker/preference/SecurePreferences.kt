package com.tikalk.worktracker.preference

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager

/**
 * Secured preferences that encrypt/decrypt the keys and values.
 * @author moshe on 2018/07/17.
 */
class SecurePreferences(context: Context, name: String, mode: Int) : SharedPreferences {

    private val delegate: SharedPreferences = context.getSharedPreferences(name, mode)

    override fun contains(key: String): Boolean {
        return delegate.contains(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return delegate.edit()
    }

    override fun getAll(): MutableMap<String, *> {
        return delegate.all
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean {
        return delegate.getBoolean(key, defValue)
    }

    override fun getFloat(key: String, defValue: Float): Float {
        return delegate.getFloat(key, defValue)
    }

    override fun getInt(key: String, defValue: Int): Int {
        return delegate.getInt(key, defValue)
    }

    override fun getLong(key: String, defValue: Long): Long {
        return delegate.getLong(key, defValue)
    }

    override fun getString(key: String, defValue: String?): String {
        return delegate.getString(key, defValue)
    }

    override fun getStringSet(key: String, defValue: Set<String>?): Set<String> {
        return delegate.getStringSet(key, defValue)
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        return delegate.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        return delegate.unregisterOnSharedPreferenceChangeListener(listener)
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