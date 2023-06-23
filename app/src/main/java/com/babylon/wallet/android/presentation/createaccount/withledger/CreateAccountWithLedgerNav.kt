package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable

private const val ROUTE_CREATE_ACCOUNT_WITH_LEDGER = "route_create_account_with_ledger"

fun NavController.createAccountWithLedger() {
    navigate(
        route = ROUTE_CREATE_ACCOUNT_WITH_LEDGER
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createAccountWithLedger(onBackClick: () -> Unit, goBackToCreateAccount: () -> Unit, onAddP2PLink: () -> Unit) {
    markAsHighPriority(ROUTE_CREATE_ACCOUNT_WITH_LEDGER)
    composable(
        route = ROUTE_CREATE_ACCOUNT_WITH_LEDGER,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        CreateAccountWithLedgerScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            goBackToCreateAccount = goBackToCreateAccount,
            onAddP2PLink = onAddP2PLink
        )
    }
}
