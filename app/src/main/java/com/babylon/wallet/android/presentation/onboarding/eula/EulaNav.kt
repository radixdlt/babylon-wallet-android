package com.babylon.wallet.android.presentation.onboarding.eula

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_EULA_SCREEN = "eula_screen"

fun NavController.navigateToEulaScreen() {
    navigate(route = ROUTE_EULA_SCREEN)
}

fun NavGraphBuilder.eulaScreen(
    onBackClick: (isWithCloudBackupEnabled: Boolean) -> Unit,
    onAccepted: (isWithCloudBackupEnabled: Boolean) -> Unit
) {
    composable(
        route = ROUTE_EULA_SCREEN
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
