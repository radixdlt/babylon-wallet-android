package com.babylon.wallet.android.presentation.account.createaccount.withledger

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.utils.Constants
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
const val ARG_NETWORK_ID = "arg_network_id"

private const val ROUTE_CREATE_ACCOUNT_WITH_LEDGER = "route_create_account_with_ledger?$ARG_NETWORK_ID={$ARG_NETWORK_ID}"

internal class CreateAccountWithLedgerArgs(
    val networkId: Int,
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle.get<Int>(ARG_NETWORK_ID))
    )
}

fun NavController.createAccountWithLedger(networkId: Int = Constants.USE_CURRENT_NETWORK) {
    navigate(
        route = "route_create_account_with_ledger?$ARG_NETWORK_ID=$networkId"
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createAccountWithLedger(
    onBackClick: () -> Unit,
    goBackToCreateAccount: () -> Unit
) {
    markAsHighPriority(ROUTE_CREATE_ACCOUNT_WITH_LEDGER)
    composable(
        route = ROUTE_CREATE_ACCOUNT_WITH_LEDGER,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        },
        arguments = listOf(
            navArgument(ARG_NETWORK_ID) {
                type = NavType.IntType
                defaultValue = Constants.USE_CURRENT_NETWORK
            }
        )
    ) {
        CreateAccountWithLedgerScreen(
            viewModel = hiltViewModel(),
            addLedgerDeviceViewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onBackClick = onBackClick,
            goBackToCreateAccount = goBackToCreateAccount
        )
    }
}
