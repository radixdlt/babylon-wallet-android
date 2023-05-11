package com.babylon.wallet.android.presentation.settings.ledgerfactorsource

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

const val ROUTE_LEDGER_FACTOR_SOURCES = "settings_ledger_factor_source"

fun NavController.settingsLedgerFactorSourcesScreen() {
    navigate(ROUTE_LEDGER_FACTOR_SOURCES)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsLedgerFactorSourcesScreen(
    onBackClick: () -> Unit,
    onAddP2PLink: () -> Unit
) {
    composable(
        route = ROUTE_LEDGER_FACTOR_SOURCES,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        LedgerFactorSourcesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onAddP2PLink = onAddP2PLink
        )
    }
}
