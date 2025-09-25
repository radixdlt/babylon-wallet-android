package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.FactorSourceId

private const val ROUTE = "ledger_hardware_wallets_route"

fun NavController.ledgerDevices() {
    navigate(ROUTE) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.ledgerDevices(
    toFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        LedgerDevicesScreen(
            viewModel = hiltViewModel(),
            toFactorSourceDetails = toFactorSourceDetails,
            onBackClick = onBackClick
        )
    }
}
