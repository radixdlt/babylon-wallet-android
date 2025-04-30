package com.babylon.wallet.android.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.babylon.wallet.android.designsystem.theme.RadixThemeConfig
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ProvideMockActionableAddressViewEntryPoint
import rdx.works.core.domain.ThemeSelection

@Composable
fun RadixWalletPreviewTheme(
    enableDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    ProvideMockActionableAddressViewEntryPoint {
        RadixWalletTheme(
            config = RadixThemeConfig(
                themeSelection = ThemeSelection.SYSTEM,
                isSystemDarkTheme = enableDarkTheme
            ),
            content = content
        )
    }
}
