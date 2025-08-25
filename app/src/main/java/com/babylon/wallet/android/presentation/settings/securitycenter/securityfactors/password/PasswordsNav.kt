package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.password

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.FactorSourceId

private const val ROUTE_PASSWORDS_SCREEN = "route_passwords_screen"

fun NavController.passwords() {
    navigate(ROUTE_PASSWORDS_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.passwords(
    onNavigateToPasswordFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_PASSWORDS_SCREEN,
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
        PasswordsScreen(
            viewModel = hiltViewModel(),
            onNavigateToPasswordFactorSourceDetails = onNavigateToPasswordFactorSourceDetails,
            onBackClick = onBackClick
        )
    }
}
