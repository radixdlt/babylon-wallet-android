package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_CLOUD_BACKUP_LOGIN = "cloud_backup_login_screen"

fun NavController.cloudBackupLoginScreen() {
    navigate(route = ROUTE_CLOUD_BACKUP_LOGIN)
}

fun NavGraphBuilder.cloudBackupLoginScreen(
    onBackClick: () -> Unit,
    onContinueToRestoreFromBackup: () -> Unit,
) {
    composable(
        route = ROUTE_CLOUD_BACKUP_LOGIN,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        CloudBackupLoginScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onContinueToRestoreFromBackup = onContinueToRestoreFromBackup
        )
    }
}
