package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.preparefactors.prepareFactors
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors.selectFactors

private const val ROUTE_SECURITY_SHIELD_ONBOARDING = "security_shield_onboarding"

fun NavController.securityShieldOnboarding() {
    navigate(ROUTE_SECURITY_SHIELD_ONBOARDING)
}

fun NavGraphBuilder.securityShieldOnboarding(
    navController: NavController
) {
    composable(
        route = ROUTE_SECURITY_SHIELD_ONBOARDING,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        SecurityShieldOnboardingScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onSelectFactors = { navController.selectFactors() },
            onSetupFactors = { navController.prepareFactors() }
        )
    }
}
