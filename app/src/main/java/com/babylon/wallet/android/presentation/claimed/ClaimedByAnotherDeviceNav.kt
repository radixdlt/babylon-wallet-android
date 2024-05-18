package com.babylon.wallet.android.presentation.claimed

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "route_claimed_by_another_device"

fun NavController.navigateToClaimedByAnotherDevice() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.claimedByAnotherDevice(
    onNavigateToOnboarding: () -> Unit
) {
    composable(route = ROUTE) {
        ClaimedByAnotherDeviceScreen(
            viewModel = hiltViewModel(),
            onNavigateToOnboarding = onNavigateToOnboarding
        )
    }
}
