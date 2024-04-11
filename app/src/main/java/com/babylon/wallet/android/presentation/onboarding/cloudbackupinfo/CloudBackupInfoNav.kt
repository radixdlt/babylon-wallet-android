package com.babylon.wallet.android.presentation.onboarding.cloudbackupinfo

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.onboarding.OnboardingViewModel

private const val ROUTE = "cloud_backup_info_screen"

fun NavController.navigateToCloudBackupInfoScreen() {
    navigate(ROUTE)
}

fun NavGraphBuilder.cloudBackupInfoScreen(
    navController: NavController,
    onBackClick: () -> Unit,
    onContinueClick: (Boolean) -> Unit
) {
    composable(route = ROUTE) {
        val backstackEntry = remember(it) {
            navController.getBackStackEntry(Screen.OnboardingDestination.route)
        }
        val sharedViewModel = hiltViewModel<OnboardingViewModel>(backstackEntry)
        CloudBackupInfoScreen(
            viewModel = sharedViewModel,
            onBackClick = onBackClick,
            onContinueClick = onContinueClick
        )
    }
}
