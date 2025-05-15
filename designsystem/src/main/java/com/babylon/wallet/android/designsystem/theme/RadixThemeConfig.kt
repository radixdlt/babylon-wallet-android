package com.babylon.wallet.android.designsystem.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.core.content.getSystemService
import rdx.works.core.domain.ThemeSelection

data class RadixThemeConfig(
    val isDarkTheme: Boolean
)

/**
 * According to this [guide](https://developer.android.com/develop/ui/views/theming/darktheme#change-themes)
 * on Android >= 31 we can use directly [UiModeManager]. The setting is persisted.
 *
 * In earlier versions we need to use
 *  - [AppCompatDelegate] to update the theme
 *  - change [MainActivity] to extend [AppCompatActivity]
 *  Be aware that this setting is not persisted when using [AppCompatDelegate], so we need to read
 *  the preference stored in datastore in order to set the correct theme when the app launches.
 *
 * Lastly the Activity's `configurationChanges` is set to `uiMode` in order to avoid recreation when
 * the theme changes.
 *
 * @see rememberRadixThemeConfig for more info about the initial setup.
 */
fun Context.applyThemeSelection(themeSelection: ThemeSelection) {
    val uiModeManager = getSystemService<UiModeManager>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        when (themeSelection) {
            ThemeSelection.LIGHT ->
                uiModeManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
            ThemeSelection.DARK ->
                uiModeManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
            ThemeSelection.SYSTEM ->
                uiModeManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
        }
    } else {
        when (themeSelection) {
            ThemeSelection.LIGHT ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeSelection.DARK ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeSelection.SYSTEM ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }
}

/**
 * Setups the [RadixThemeConfig] based on the current [ThemeSelection] and the system's setting.
 *
 * On Android >= 31 we can simply use [isSystemInDarkTheme] since it is persisted and updated when
 * [UiModeManager] updates the value.
 *
 * In earlier versions we can react to such theme changes by observing [ThemeSelection]. This
 * in conjunction with the [AppCompatDelegate.getDefaultNightMode] can setup/reset the current
 * theme. Keep in mind when the activity starts for the fist time the default night mode is set to
 * [AppCompatDelegate.MODE_NIGHT_UNSPECIFIED]. In that case we need to set it up again according to
 * [ThemeSelection].
 */
@Composable
fun rememberRadixThemeConfig(selectedTheme: ThemeSelection?): RadixThemeConfig {
    val isSystemInDarkTheme = isSystemInDarkTheme()

    val isUsingDarkTheme = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        remember(selectedTheme, isSystemInDarkTheme) {
            if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
                // Activity probably created. Set the value based on the selected theme
                when (selectedTheme) {
                    ThemeSelection.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    ThemeSelection.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    ThemeSelection.SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    else -> {
                        // Ignore initial state, wait until themeSelection is read from preferences
                    }
                }
            }

            // Get the updated system night mode state and infer the theme
            val appNightMode = AppCompatDelegate.getDefaultNightMode()
            appNightMode == AppCompatDelegate.MODE_NIGHT_YES ||
                    (appNightMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM && isSystemInDarkTheme)
        }
    } else {
        // In newer versions (API >= 31) system preference is persisted.
        // No need to read from themeSelection
        isSystemInDarkTheme
    }

    return remember(isUsingDarkTheme) {
        RadixThemeConfig(isDarkTheme = isUsingDarkTheme)
    }
}