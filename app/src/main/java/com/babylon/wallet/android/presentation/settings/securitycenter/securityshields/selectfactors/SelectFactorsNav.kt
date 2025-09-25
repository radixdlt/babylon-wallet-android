package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess.regularAccess

private const val ROUTE_SELECT_FACTORS = "select_factors"

fun NavController.selectFactors() {
    navigate(ROUTE_SELECT_FACTORS)
}

fun NavGraphBuilder.selectFactors(
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
            toRegularAccess = { navController.regularAccess() }
        )
    }
}
