package com.babylon.wallet.android.presentation.dappdir

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "dApp-directory"

fun NavController.dAppDirectory() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.dAppDirectory() {
    composable(
        route = ROUTE
    ) {
        DAppDirectoryScreen()
    }
}