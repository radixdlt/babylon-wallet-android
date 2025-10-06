package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shields

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.createSecurityShield
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails.securityShieldDetails

private const val ROUTE_SECURITY_SHIELDS = "security_shields"

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
            onNavigateToSecurityShieldDetails = { securityShieldId ->
                navController.securityShieldDetails(securityShieldId)
            },
            onCreateNewSecurityShieldClick = { navController.createSecurityShield() },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBackClick = { navController.navigateUp() },
        )
    }
}
