package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.recovery

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog

const val ROUTE_SETUP_RECOVERY = "setup_recovery"

fun NavController.setupRecoveryScreen() {
    navigate(ROUTE_SETUP_RECOVERY)
}

fun NavGraphBuilder.setupRecoveryScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_SETUP_RECOVERY,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SetupRecoveryScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) }
        )
    }
}
