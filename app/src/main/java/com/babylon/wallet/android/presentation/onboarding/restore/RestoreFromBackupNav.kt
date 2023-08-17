@file:OptIn(ExperimentalAnimationApi::class)

package com.babylon.wallet.android.presentation.onboarding.restore

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

private const val ROUTE_RESTORE_FROM_BACKUP = "restore_from_backup"

fun NavController.restoreFromBackupScreen() {
    navigate(route = ROUTE_RESTORE_FROM_BACKUP)
}

fun NavGraphBuilder.restoreFromBackupScreen(
    onBack: () -> Unit,
    onRestored: (Boolean) -> Unit
) {
    composable(
        route = ROUTE_RESTORE_FROM_BACKUP,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        RestoreFromBackupScreen(
            viewModel = hiltViewModel(),
            onBack = onBack,
            onRestored = onRestored
        )
    }
}
