package com.example.util

import android.content.Context

object AppPreferences {
    private const val PREF_NAME = "mb_traker_app_prefs"
    private const val KEY_DUMMY_DATA_CLEARED = "dummy_data_cleared"
    private const val KEY_APP_INSTALL_DATE = "app_install_date"
    private const val KEY_ONBOARDING_COMPLETED = "milo_onboarding_completed"

    fun isOnboardingCompleted(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, completed)
            .apply()
    }

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

    private const val KEY_BATTERY_SAVER_MODE = "battery_saver_mode"
    private const val KEY_BATTERY_SAVER_THRESHOLD = "battery_saver_threshold"

    fun getBatterySaverMode(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BATTERY_SAVER_MODE, "AUTO") ?: "AUTO"
    }

    fun setBatterySaverMode(context: Context, mode: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BATTERY_SAVER_MODE, mode)
            .apply()
    }

    fun getBatterySaverThreshold(context: Context): Int {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_BATTERY_SAVER_THRESHOLD, 20)
    }

    fun setBatterySaverThreshold(context: Context, threshold: Int) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_BATTERY_SAVER_THRESHOLD, threshold)
            .apply()
    }
}
