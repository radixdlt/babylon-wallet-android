package com.babylon.wallet.android.presentation.dapp.login

import androidx.compose.runtime.Composable
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressScreen

@Composable
fun ChooseDAppLoginScreen(
    viewModel: ChooseDAppLoginViewModel,
    onBackClick: () -> Unit,
    onContinueClick: () -> Unit
) {

    val state = viewModel.state

    if (state.loading) {
        FullscreenCircularProgressScreen()
    }

    state.dAppData?.let { dAppData ->
        ChooseDAppLoginContent(
            onBackClick = onBackClick,
            onContinueClick = onContinueClick,
            selected = viewModel.selected,
            onSelectedChange = viewModel::onSelectChange,
            dAppData = dAppData
        )
    }
}
