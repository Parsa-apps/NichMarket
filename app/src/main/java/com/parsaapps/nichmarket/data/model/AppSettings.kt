package com.parsaapps.nichmarket.data.model

/** Preferences are local to the device. Unknown stored theme names fall back to SYSTEM. */
enum class AppTheme { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val largeText: Boolean = false,
    val autoAdvance: Boolean = false,
    val hideTimer: Boolean = false,
    val dailyGoal: Int = 10
)
