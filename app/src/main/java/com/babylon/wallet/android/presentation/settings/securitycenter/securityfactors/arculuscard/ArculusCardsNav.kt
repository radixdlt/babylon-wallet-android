package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.FactorSourceId

private const val ROUTE_ARCULUS_CARDS_SCREEN = "route_arculus_cards_screen"

fun NavController.arculusCards() {
    navigate(ROUTE_ARCULUS_CARDS_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.arculusCards(
    onNavigateToArculusFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_ARCULUS_CARDS_SCREEN,
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
        ArculusCardsScreen(
            viewModel = hiltViewModel(),
            onNavigateToArculusFactorSourceDetails = onNavigateToArculusFactorSourceDetails,
            onBackClick = onBackClick
        )
    }
}
