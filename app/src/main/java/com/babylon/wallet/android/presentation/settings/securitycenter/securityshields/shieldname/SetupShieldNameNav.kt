package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldname

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldcreated.shieldCreated

private const val ROUTE_SETUP_SHIELD_NAME = "setup_shield_name"

fun NavController.setupShieldName() {
    navigate(ROUTE_SETUP_SHIELD_NAME)
}

fun NavGraphBuilder.setupShieldName(
    navController: NavController
) {
    composable(
        route = ROUTE_SETUP_SHIELD_NAME,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SetupShieldNameScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onShieldCreated = { id -> navController.shieldCreated(id) }
        )
    }
}
