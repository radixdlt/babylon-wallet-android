package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

private const val ROUTE_BIOMETRICS_PIN_SCREEN = "route_biometrics_pin_screen"

fun NavController.biometricsPin() {
    navigate(ROUTE_BIOMETRICS_PIN_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.biometricsPin(
    onBackClick: () -> Unit,
    onNavigateToDeviceFactorSourceDetails: () -> Unit,
    onNavigateToAddBiometricPin: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    composable(
        route = ROUTE_BIOMETRICS_PIN_SCREEN,
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
        BiometricsPinScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onNavigateToDeviceFactorSourceDetails = onNavigateToDeviceFactorSourceDetails,
            onNavigateToAddBiometricPin = onNavigateToAddBiometricPin,
            onInfoClick = onInfoClick
        )
    }
}