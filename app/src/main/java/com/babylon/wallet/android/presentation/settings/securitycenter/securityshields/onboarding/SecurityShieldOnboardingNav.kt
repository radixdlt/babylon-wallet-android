package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable

const val ROUTE_SECURITY_SHIELD_ONBOARDING = "security_shield_onboarding"

fun NavController.securityShieldOnboardingScreen(navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(ROUTE_SECURITY_SHIELD_ONBOARDING, navOptionsBuilder)
}

@Suppress("LongParameterList")
fun NavGraphBuilder.securityShieldOnboardingScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_SECURITY_SHIELD_ONBOARDING,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        SecurityShieldOnboardingScreen(
            viewModel = hiltViewModel(),
            onDismiss = onBackClick
        )
    }
}