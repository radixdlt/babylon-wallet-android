package com.babylon.wallet.android.presentation.boot

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE_BOOT_ERROR = "boot_error"

fun NavController.navigateToBootError() {
    navigate(route = ROUTE_BOOT_ERROR)
}


fun NavGraphBuilder.bootError(
    onFinishProcess: () -> Unit
) {
    composable(
        route = ROUTE_BOOT_ERROR
    ) {
        SargonBootErrorScreen(
            viewModel = hiltViewModel(),
            onFinish = onFinishProcess
        )
    }
}
