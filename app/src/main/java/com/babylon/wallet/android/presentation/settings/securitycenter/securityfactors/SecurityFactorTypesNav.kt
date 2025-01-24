package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.FactorSourceKind

private const val ROUTE_SECURITY_FACTOR_TYPES_SCREEN = "settings_security_factor_types_screen"

fun NavController.securityFactorTypes() {
    navigate(ROUTE_SECURITY_FACTOR_TYPES_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.securityFactorTypes(
    onBackClick: () -> Unit,
    onSecurityFactorTypeClick: (FactorSourceKind) -> Unit
) {
    composable(
        route = ROUTE_SECURITY_FACTOR_TYPES_SCREEN,
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
        SecurityFactorTypesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onSecurityFactorTypeClick = onSecurityFactorTypeClick
        )
    }
}
