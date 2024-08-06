package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import rdx.works.profile.domain.backup.BackupType

const val ROUTE_RESTORE_FROM_BACKUP = "route_restore_from_backup"

fun NavController.restoreFromBackupScreen() {
    navigate(route = ROUTE_RESTORE_FROM_BACKUP)
}

fun NavGraphBuilder.restoreFromBackupScreen(
    onBackClick: () -> Unit,
    onRestoreConfirmed: (BackupType) -> Unit,
    onOtherRestoreOptionsClick: () -> Unit
) {
    composable(
        route = ROUTE_RESTORE_FROM_BACKUP,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        RestoreFromBackupScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onRestoreConfirmed = onRestoreConfirmed,
            onOtherRestoreOptionsClick = onOtherRestoreOptionsClick
        )
    }
}
