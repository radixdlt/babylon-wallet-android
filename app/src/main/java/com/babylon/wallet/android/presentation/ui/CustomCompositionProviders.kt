package com.babylon.wallet.android.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressViewEntryPoint
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.LocalActionableAddressViewEntryPoint

@Composable
fun CustomCompositionProviders(
    isBalanceVisible: Boolean,
    actionableAddressViewEntryPoint: ActionableAddressViewEntryPoint,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalBalanceVisibility provides isBalanceVisible) {
        CompositionLocalProvider(LocalActionableAddressViewEntryPoint provides actionableAddressViewEntryPoint) {
            content()
        }
    }
}
