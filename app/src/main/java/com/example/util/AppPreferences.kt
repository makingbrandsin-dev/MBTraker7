package com.example.util

import android.content.Context

object AppPreferences {
    private const val PREF_NAME = "mb_traker_app_prefs"
    private const val KEY_DUMMY_DATA_CLEARED = "dummy_data_cleared"
    private const val KEY_APP_INSTALL_DATE = "app_install_date"

    fun getAppInstallDate(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        var date = prefs.getString(KEY_APP_INSTALL_DATE, null)
        if (date.isNullOrBlank()) {
            date = getCurrentDateString()
            prefs.edit().putString(KEY_APP_INSTALL_DATE, date).apply()
        }
        return date
    }

    fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun isDummyDataCleared(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_DUMMY_DATA_CLEARED, false)
    }

    fun setDummyDataCleared(context: Context, cleared: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_DUMMY_DATA_CLEARED, cleared)
            .apply()
    }
}
