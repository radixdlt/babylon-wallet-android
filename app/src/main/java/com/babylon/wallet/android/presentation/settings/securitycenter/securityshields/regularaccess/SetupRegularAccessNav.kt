package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog

const val ROUTE_SETUP_REGULAR_ACCESS = "setup_regular_access"

fun NavController.regularAccessScreen() {
    navigate(ROUTE_SETUP_REGULAR_ACCESS)
}

fun NavGraphBuilder.regularAccessScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_SETUP_REGULAR_ACCESS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SetupRegularAccessScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onContinue = {
                // TODO navigate to recovery role screen
            }
        )
    }
}
