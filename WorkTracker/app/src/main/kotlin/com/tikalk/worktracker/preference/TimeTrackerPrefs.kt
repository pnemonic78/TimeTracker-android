package com.tikalk.worktracker.preference

import android.content.Context
import android.preference.PreferenceManager
import com.tikalk.worktracker.auth.model.BasicCredentials

/**
 * Time Tracker preferences.
 * @author moshe on 2018/05/13.
 */
class TimeTrackerPrefs(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val BASIC_CREDENTIALS_REALM = "credentials.basic.realm"
    private val BASIC_CREDENTIALS_USER = "credentials.basic.username"
    private val BASIC_CREDENTIALS_PASSWORD = "credentials.basic.password"

    var basicCredentials: BasicCredentials = BasicCredentials("", "", "")
        get() {
            field.realm = prefs.getString(BASIC_CREDENTIALS_REALM, "")
            field.username = prefs.getString(BASIC_CREDENTIALS_USER, "")
            field.password = prefs.getString(BASIC_CREDENTIALS_PASSWORD, "")
            return field
        }
        set(value) {
            field.realm = value.realm
            field.username = value.username
            field.password = value.password
            prefs.edit()
                    .putString(BASIC_CREDENTIALS_REALM, value.realm)
                    .putString(BASIC_CREDENTIALS_REALM, value.username)
                    .putString(BASIC_CREDENTIALS_REALM, value.password)
                    .apply()
        }
}