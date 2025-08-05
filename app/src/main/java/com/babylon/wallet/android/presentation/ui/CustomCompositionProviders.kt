package com.babylon.wallet.android.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.babylon.wallet.android.presentation.LocalBalanceVisibility
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.ActionableAddressViewEntryPoint
import com.babylon.wallet.android.presentation.ui.composables.actionableaddress.LocalActionableAddressViewEntryPoint
import com.babylon.wallet.android.presentation.ui.composables.card.FactorSourceCardViewEntryPoint
import com.babylon.wallet.android.presentation.ui.composables.card.LocalFactorSourceCardViewEntryPoint

@Composable
fun CustomCompositionProviders(
    isBalanceVisible: Boolean,
    actionableAddressViewEntryPoint: ActionableAddressViewEntryPoint,
    factorSourceCardViewEntryPoint: FactorSourceCardViewEntryPoint,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalBalanceVisibility provides isBalanceVisible) {
        CompositionLocalProvider(LocalActionableAddressViewEntryPoint provides actionableAddressViewEntryPoint) {
            CompositionLocalProvider(LocalFactorSourceCardViewEntryPoint provides factorSourceCardViewEntryPoint) {
                content()
            }
        }
    }
}
