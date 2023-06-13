package com.babylon.wallet.android.presentation.transfer

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable
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

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.transferScreen(
    onBackClick: () -> Unit,
) {
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
