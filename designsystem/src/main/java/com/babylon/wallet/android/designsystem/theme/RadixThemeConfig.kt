package com.babylon.wallet.android.designsystem.theme

import rdx.works.core.domain.ThemeSelection

data class RadixThemeConfig(
    private val themeSelection: ThemeSelection = ThemeSelection.DEFAULT,
    private val isSystemDarkTheme: Boolean
) {
    val isDarkTheme: Boolean
        get() = when (themeSelection) {
            ThemeSelection.LIGHT -> false
            ThemeSelection.DARK -> true
            ThemeSelection.SYSTEM -> isSystemDarkTheme
        }
}
