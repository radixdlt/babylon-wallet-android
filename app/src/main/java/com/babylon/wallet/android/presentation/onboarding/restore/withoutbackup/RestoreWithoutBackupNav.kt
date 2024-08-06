package com.babylon.wallet.android.presentation.onboarding.restore.withoutbackup

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE_RESTORE_WITHOUT_BACKUP = "restore_without_backup"

fun NavController.restoreWithoutBackupScreen() {
    navigate(route = ROUTE_RESTORE_WITHOUT_BACKUP)
}

fun NavGraphBuilder.restoreWithoutBackupScreen(
    onBack: () -> Unit,
    onRestoreConfirmed: () -> Unit,
    onNewUserConfirmClick: () -> Unit,
) {
    composable(
        route = ROUTE_RESTORE_WITHOUT_BACKUP,
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
        RestoreWithoutBackupScreen(
            viewModel = hiltViewModel(),
            onBack = onBack,
            onRestoreConfirmed = onRestoreConfirmed,
            onNewUserConfirmClick = onNewUserConfirmClick
        )
    }
}
