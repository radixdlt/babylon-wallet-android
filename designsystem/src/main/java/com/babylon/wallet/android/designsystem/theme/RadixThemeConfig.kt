package com.babylon.wallet.android.designsystem.theme

import rdx.works.core.domain.ThemeSelection

data class RadixThemeConfig(
    val themeSelection: ThemeSelection = ThemeSelection.DEFAULT,
    val isSystemDarkTheme: Boolean
) {
    val isDarkTheme: Boolean
        get() = when (themeSelection) {
            ThemeSelection.LIGHT -> false
            ThemeSelection.DARK -> true
            ThemeSelection.SYSTEM -> isSystemDarkTheme
        }
}