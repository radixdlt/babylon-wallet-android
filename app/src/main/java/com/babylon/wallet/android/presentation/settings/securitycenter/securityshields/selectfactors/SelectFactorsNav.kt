package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog

const val ROUTE_SELECT_FACTORS = "select_factors"

fun NavController.selectFactorsScreen() {
    navigate(ROUTE_SELECT_FACTORS)
}

fun NavGraphBuilder.selectFactorsScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_SELECT_FACTORS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SelectFactorsScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBuildShield = {
                // TODO navigate to build shield screen for access role
            }
        )
    }
}
