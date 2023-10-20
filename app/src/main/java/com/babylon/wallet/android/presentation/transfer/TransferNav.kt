package com.babylon.wallet.android.presentation.transfer

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
internal const val ARG_ACCOUNT_ID = "arg_account_id"
const val ROUTE_TRANSFER = "transfer/{$ARG_ACCOUNT_ID}"

internal class TransferArgs(val accountId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_ACCOUNT_ID]) as String
    )
}

fun NavController.transfer(accountId: String) {
    navigate("transfer/$accountId")
}

fun NavGraphBuilder.transferScreen(
    onBackClick: () -> Unit,
) {
    markAsHighPriority(ROUTE_TRANSFER)
    composable(
        route = ROUTE_TRANSFER,
        arguments = listOf(
            navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
        )
    ) {
        TransferScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
