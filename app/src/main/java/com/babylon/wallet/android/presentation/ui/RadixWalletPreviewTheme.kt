package com.babylon.wallet.android.presentation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.babylon.wallet.android.designsystem.theme.RadixWalletTheme
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ProvideMockActionableAddressViewEntryPoint

@Composable
fun RadixWalletPreviewTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    ProvideMockActionableAddressViewEntryPoint {
        RadixWalletTheme(
            darkTheme = darkTheme,
            content = content
        )
    }
}
