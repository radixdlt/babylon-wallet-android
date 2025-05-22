package com.babylon.wallet.android.presentation.dappdir

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.AccountAddress

private const val ROUTE = "dApp-directory"

fun NavController.dAppDirectory() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.dAppDirectory(
    onDAppClick: (AccountAddress) -> Unit
) {
    composable(
        route = ROUTE
    ) {
        DAppDirectoryScreen(
            viewModel = hiltViewModel(),
            onDAppClick = onDAppClick
        )
    }
}
