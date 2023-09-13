@file:OptIn(ExperimentalAnimationApi::class)

package com.babylon.wallet.android.presentation.onboarding.eula

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

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
