package com.babylon.wallet.android.presentation.createaccount.addledger

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

const val ROUTE_ADD_LEDGER = "add_ledger_route"

fun NavController.addLedger() {
    navigate(
        route = ROUTE_ADD_LEDGER
    )
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.addLedger(onBackClick: () -> Unit, onAddP2PLink: () -> Unit, goBackToCreateAccount: () -> Unit) {
    composable(
        route = ROUTE_ADD_LEDGER,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        AddLedgerScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onAddP2PLink = onAddP2PLink,
            goBackToCreateAccount = goBackToCreateAccount
        )
    }
}
