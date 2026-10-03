package com.hkode.h3nrican3.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREFS_NAME = "hkode_theme_prefs"
    private const val KEY_THEME = "selected_theme"

    const val THEME_SYSTEM = "SYSTEM"
    const val THEME_LIGHT = "LIGHT"
    const val THEME_DARK = "DARK"

    fun init(context: Context) {
        val theme = getSavedTheme(context)
        applyTheme(theme)
    }

    fun getSavedTheme(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
    }

    fun setSavedTheme(context: Context, theme: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME, theme).apply()
        applyTheme(theme)
    }

    fun applyTheme(theme: String) {
        val mode = when (theme) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    fun resetToDefault(context: Context) {
        setSavedTheme(context, THEME_SYSTEM)
    }
}
