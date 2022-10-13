package com.babylon.wallet.android.presentation.dapp.login.view

import androidx.compose.runtime.Composable
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressScreen
import com.babylon.wallet.android.presentation.dapp.login.viewmodel.ChooseDAppLoginViewModel

@Composable
fun ChooseDAppLoginScreen(
    viewModel: ChooseDAppLoginViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {

    if (viewModel.uiState.loading) {
        FullscreenCircularProgressScreen()
    }

    viewModel.uiState.dAppData?.let { dAppData ->
        ChooseDAppLoginContent(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick,
            selected = viewModel.selected,
            onSelectedChange = viewModel::onSelectChange,
            dAppData = dAppData
        )
    }
}
