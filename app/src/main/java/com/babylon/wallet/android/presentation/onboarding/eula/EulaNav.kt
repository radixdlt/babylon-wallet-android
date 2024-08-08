package com.babylon.wallet.android.presentation.onboarding.eula

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_EULA_SCREEN = "route_eula_screen"

fun NavController.navigateToEulaScreen() {
    navigate(route = ROUTE_EULA_SCREEN)
}

fun NavGraphBuilder.eulaScreen(
    onBackClick: (isWithCloudBackupEnabled: Boolean) -> Unit,
    onAccepted: (isWithCloudBackupEnabled: Boolean) -> Unit
) {
    composable(
        route = ROUTE_EULA_SCREEN,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        EulaScreen(
            viewModel = hiltViewModel(),
            onBackClick = { isWithCloudBackupEnabled ->
                onBackClick(isWithCloudBackupEnabled)
            },
            onAccepted = { isWithCloudBackupEnabled ->
                onAccepted(isWithCloudBackupEnabled)
            }

        )
    }
}
