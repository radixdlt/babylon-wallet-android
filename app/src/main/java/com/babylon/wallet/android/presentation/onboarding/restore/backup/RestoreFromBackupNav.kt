@file:OptIn(ExperimentalAnimationApi::class)

package com.babylon.wallet.android.presentation.onboarding.restore.backup

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_RESTORE_FROM_BACKUP = "restore_from_backup"

fun NavController.restoreFromBackupScreen() {
    navigate(route = ROUTE_RESTORE_FROM_BACKUP)
}

fun NavGraphBuilder.restoreFromBackupScreen(
    onBack: () -> Unit,
    onRestoreConfirmed: (Boolean) -> Unit,
    onOtherRestoreOptionsClick: () -> Unit
) {
    composable(
        route = ROUTE_RESTORE_FROM_BACKUP,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        RestoreFromBackupScreen(
            viewModel = hiltViewModel(),
            onBack = onBack,
            onRestoreConfirmed = onRestoreConfirmed,
            onOtherRestoreOptionsClick = onOtherRestoreOptionsClick
        )
    }
}
