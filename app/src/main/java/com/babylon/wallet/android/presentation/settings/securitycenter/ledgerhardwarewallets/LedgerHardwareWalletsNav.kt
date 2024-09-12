package com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

private const val ROUTE = "ledger_hardware_wallets_route"

fun NavController.ledgerHardwareWalletsScreen() {
    navigate(ROUTE) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.ledgerHardwareWalletsScreen(
    onInfoClick: (GlossaryItem) -> Unit,
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
        LedgerHardwareWalletsScreen(
            viewModel = hiltViewModel(),
            addLedgerDeviceViewModel = hiltViewModel(),
            addLinkConnectorViewModel = hiltViewModel(),
            onInfoClick = onInfoClick,
            onBackClick = onBackClick
        )
    }
}
