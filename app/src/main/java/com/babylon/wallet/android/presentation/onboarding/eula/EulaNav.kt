package com.babylon.wallet.android.presentation.onboarding.eula

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "eula_screen"

fun NavController.navigateToEulaScreen() {
    navigate(ROUTE)
}

fun NavGraphBuilder.eulaScreen(
    onBack: () -> Unit,
    onAccepted: () -> Unit
) {
    composable(
        route = ROUTE
    ) {
        EulaScreen(
            onBack = onBack,
            onAccepted = onAccepted
        )
    }
}
