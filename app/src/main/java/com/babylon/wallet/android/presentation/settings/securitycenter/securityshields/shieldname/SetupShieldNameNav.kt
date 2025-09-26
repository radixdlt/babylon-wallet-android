package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldname

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.CreateSecurityShieldSharedViewModel
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.ROUTE_CREATE_SECURITY_SHIELD_GRAPH
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
        val parentEntry = remember(it) { navController.getBackStackEntry(ROUTE_CREATE_SECURITY_SHIELD_GRAPH) }
        val sharedVM = hiltViewModel<CreateSecurityShieldSharedViewModel>(parentEntry)

        SetupShieldNameScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onShieldCreated = { id ->
                val hasEntityContext = sharedVM.args.address != null

                if (hasEntityContext) {
                    navController.popBackStack(ROUTE_CREATE_SECURITY_SHIELD_GRAPH, true)
                } else {
                    navController.shieldCreated(id)
                }
            }
        )
    }
}
