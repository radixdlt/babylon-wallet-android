package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.preparefactors

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor.addAnyFactor
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor.addHardwareDevice

private const val ROUTE_PREPARE_FACTORS = "prepare_factors"

fun NavController.prepareFactors() {
    navigate(ROUTE_PREPARE_FACTORS)
}

fun NavGraphBuilder.prepareFactors(
    navController: NavController
) {
    composable(
        route = ROUTE_PREPARE_FACTORS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        PrepareFactorsScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            toAddAnotherFactor = { navController.addAnyFactor() },
            toAddHardwareDevice = { navController.addHardwareDevice() }
        )
    }
}
