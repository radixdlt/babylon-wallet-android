package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ROUTE_APPLY_SHIELD_GRAPH
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply.applyShield
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.ApplyShieldSharedViewModel

private const val ROUTE_CHOOSE_PERSONAS = "choose_personas"

fun NavController.choosePersonas() {
    navigate(ROUTE_CHOOSE_PERSONAS)
}

fun NavGraphBuilder.choosePersonas(
    navController: NavController
) {
    composable(
        route = ROUTE_CHOOSE_PERSONAS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        val parentEntry = remember(it) { navController.getBackStackEntry(ROUTE_APPLY_SHIELD_GRAPH) }
        val sharedVM = hiltViewModel<ApplyShieldSharedViewModel>(parentEntry)

        ChoosePersonasScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onSelected = { addresses ->
                sharedVM.onPersonasSelected(addresses)
                navController.applyShield()
            }
        )
    }
}
