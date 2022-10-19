package com.babylon.wallet.android.presentation.dapp.account

import androidx.compose.runtime.Composable
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressScreen

@Composable
fun ChooseDAppAccountScreen(
    viewModel: ChooseDAppAccountViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {

    val state = viewModel.state
    val selectedIndexes = viewModel.selectedIndexes

    if (state.loading) {
        FullscreenCircularProgressScreen()
    }

    state.dAppAccountsData?.let { dAppAccountsData ->
        ChooseDAppAccountContent(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick,
            imageUrl = dAppAccountsData.imageUrl,
            dAppAccounts = dAppAccountsData.dAppAccounts,
            dAppSelectedIndexes = selectedIndexes,
            onDAppAccountSelected = viewModel::onAccountSelect
        )
    }
}
