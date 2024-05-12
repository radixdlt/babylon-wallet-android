package com.babylon.wallet.android.presentation.onboarding.cloudbackup

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_CONNECT_CLOUD_BACKUP = "route_connect_cloud_backup_screen"

fun NavController.connectCloudBackupScreen() {
    navigate(ROUTE_CONNECT_CLOUD_BACKUP)
}

fun NavGraphBuilder.connectCloudBackupScreen(
    onBackClick: () -> Unit,
    onContinueToCreateAccount: () -> Unit,
    onSkipClick: () -> Unit
) {
    composable(
        route = ROUTE_CONNECT_CLOUD_BACKUP,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        ConnectCloudBackupScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onContinueToCreateAccount = onContinueToCreateAccount,
            onSkipClick = onSkipClick
        )
    }
}
