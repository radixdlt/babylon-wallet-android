package com.babylon.wallet.android.designsystem.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/**
 * Changes the theme of the status bar and navigation bar, while it also enables edge to edge.
 *
 * @param isDarkThemeEnabled Changes according to the system and user selection
 * @param forceDarkStatusBar Is used in places were we need to override the theme and apply a
 * dark status bar. For example when the dev banner is visible then the status bar should
 * be dark. Another example is on Account screen were the background of the status bar is light as
 * it uses the gradient of the account. In that case the status bar icons should be dark.
 */
fun ComponentActivity.edgeToEdge(
    isDarkThemeEnabled: Boolean,
    forceDarkStatusBar: Boolean
) {
    enableEdgeToEdge(
        statusBarStyle = if (forceDarkStatusBar) {
            SystemBarStyle.dark(
                scrim = android.graphics.Color.TRANSPARENT
            )
        } else {
            SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
                detectDarkMode = {
                    isDarkThemeEnabled
                }
            )
        },
        navigationBarStyle = SystemBarStyle.auto(
            lightScrim = android.graphics.Color.TRANSPARENT,
            darkScrim = android.graphics.Color.TRANSPARENT,
            detectDarkMode = {
                isDarkThemeEnabled
            }
        )
    )
}
