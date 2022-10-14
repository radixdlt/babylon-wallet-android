package com.babylon.wallet.android.presentation.dapp.connectionrequest

import androidx.compose.runtime.Composable
import com.babylon.wallet.android.presentation.common.FullscreenCircularProgressScreen

@Composable
fun DAppConnectionRequestScreen(
    viewModel: DAppConnectionRequestViewModel,
    onCloseClick: () -> Unit,
    onContinueClick: () -> Unit
) {
    val state = viewModel.state

    if (state.loading) {
        FullscreenCircularProgressScreen()
    }

    state.dAppConnectionData?.let { dAppConnectionData ->
        DAppConnectionRequestContent(
            onCloseClick = onCloseClick,
            onContinueClick = onContinueClick,
            imageUrl = dAppConnectionData.imageUrl,
            labels = dAppConnectionData.labels
        )
    }
}
