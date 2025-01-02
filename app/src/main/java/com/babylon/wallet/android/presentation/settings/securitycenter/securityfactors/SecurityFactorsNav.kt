package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.SettingsItem

private const val ROUTE_SECURITY_FACTORS_SCREEN = "settings_security_factors_screen"

fun NavController.securityFactors() {
    navigate(ROUTE_SECURITY_FACTORS_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.securityFactors(
    onBackClick: () -> Unit,
    onSecurityFactorSettingItemClick: (SettingsItem.SecurityFactorsSettingsItem) -> Unit
) {
    composable(
        route = ROUTE_SECURITY_FACTORS_SCREEN,
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
        SecurityFactorsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onSecurityFactorSettingItemClick = onSecurityFactorSettingItemClick
        )
    }
}
