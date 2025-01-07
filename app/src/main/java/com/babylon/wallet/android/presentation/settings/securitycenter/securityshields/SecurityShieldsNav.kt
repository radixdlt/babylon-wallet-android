package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.addfactor.addFactorScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.factorsready.factorsReadyScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding.securityShieldOnboardingScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.preparefactors.prepareFactorsScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectfactors.selectFactorsScreen

const val ROUTE_SECURITY_SHIELDS = "security_shields"
const val ROUTE_SECURITY_SHIELDS_GRAPH = "security_shields_graph"

fun NavGraphBuilder.securityShieldsNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_SECURITY_SHIELDS,
        route = ROUTE_SECURITY_SHIELDS_GRAPH
    ) {
        securityShieldsScreen(navController)

        securityShieldOnboardingScreen(navController)

        prepareFactorsScreen(navController)

        addFactorScreen(navController)

        factorsReadyScreen(navController)

        selectFactorsScreen(navController)
    }
}

fun NavController.securityShieldsScreen(navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(ROUTE_SECURITY_SHIELDS, navOptionsBuilder)
}

fun NavGraphBuilder.securityShieldsScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_SECURITY_SHIELDS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SecurityShieldsScreen(
            viewModel = hiltViewModel(),
            onBackClick = { navController.navigateUp() },
            onCreateShieldClick = { navController.securityShieldOnboardingScreen() }
        )
    }
}
