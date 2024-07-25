package com.babylon.wallet.android.presentation.onboarding.cloudbackup

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

private const val ARG_CONNECT_MODE = "connect_mode"
const val ROUTE_CONNECT_CLOUD_BACKUP = "route_connect_cloud_backup_screen/{$ARG_CONNECT_MODE}"

internal class ConnectCloudBackupArgs private constructor(
    val mode: ConnectCloudBackupViewModel.ConnectMode
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        mode = ConnectCloudBackupViewModel.ConnectMode.valueOf(requireNotNull(savedStateHandle.get<String>(ARG_CONNECT_MODE)))
    )
}

fun NavController.connectCloudBackupScreen(connectMode: ConnectCloudBackupViewModel.ConnectMode, popToRoute: String? = null) {
    navigate("route_connect_cloud_backup_screen/${connectMode.name}") {
        launchSingleTop = true
        popToRoute?.let { route ->
            popUpTo(route) {
                inclusive = true
            }
        }
    }
}

fun NavGraphBuilder.connectCloudBackupScreen(
    onBackClick: () -> Unit,
    onProceed: (mode: ConnectCloudBackupViewModel.ConnectMode, isCloudBackupEnabled: Boolean) -> Unit
) {
    composable(
        route = ROUTE_CONNECT_CLOUD_BACKUP,
        arguments = listOf(
            navArgument(ARG_CONNECT_MODE) {
                type = NavType.StringType
            }
        ),
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
            onProceed = onProceed
        )
    }
}
