package com.babylon.wallet.android.presentation.dapp.connectionrequest.view

import androidx.compose.runtime.Composable
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressScreen
import com.babylon.wallet.android.presentation.dapp.connectionrequest.viewmodel.DAppConnectionRequestViewModel

@Composable
fun DAppConnectionRequestScreen(
    viewModel: DAppConnectionRequestViewModel,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    if (viewModel.uiState.loading) {
        FullscreenCircularProgressScreen()
    }

    viewModel.uiState.dAppConnectionData?.let { dAppConnectionData ->
        DAppConnectionRequestContent(
            onCloseClick = onCloseClick,
            onContinueClick = onContinueClick,
            dAppConnectionData = dAppConnectionData
        )
    }
}
