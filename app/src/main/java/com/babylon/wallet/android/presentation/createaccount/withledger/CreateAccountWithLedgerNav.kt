package com.babylon.wallet.android.presentation.createaccount.withledger

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

const val ROUTE_CREATE_ACCOUNT_WITH_LEDGER = "route_create_account_with_ledger"

fun NavController.createAccountWithLedger() {
    navigate(
        route = ROUTE_CREATE_ACCOUNT_WITH_LEDGER
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.createAccountWithLedger(onBackClick: () -> Unit, onAddP2PLink: () -> Unit, goBackToCreateAccount: () -> Unit) {
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
            onAddP2PLink = onAddP2PLink,
            goBackToCreateAccount = goBackToCreateAccount
        )
    }
}
