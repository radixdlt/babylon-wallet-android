package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ROUTE_APPLY_SHIELD_GRAPH
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.ApplyShieldSharedViewModel

private const val ROUTE_APPLY_SHIELD = "apply_shield"

fun NavController.applyShield() {
    navigate(ROUTE_APPLY_SHIELD)
}

fun NavGraphBuilder.applyShield(
    navController: NavController
) {
    composable(
        route = ROUTE_APPLY_SHIELD,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        val parentEntry = remember(it) { navController.getBackStackEntry(ROUTE_APPLY_SHIELD_GRAPH) }
        val sharedVM = hiltViewModel<ApplyShieldSharedViewModel>(parentEntry)
        val sharedState by sharedVM.state.collectAsStateWithLifecycle()

        ApplyShieldScreen(
            viewModel = hiltViewModel(),
            securityStructureId = sharedState.securityStructureId,
            entityAddresses = sharedState.allAddresses,
            onDismiss = { navController.popBackStack() },
            onShieldApplied = { navController.popBackStack(ROUTE_APPLY_SHIELD_GRAPH, true) }
        )
    }
}
