package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.personas

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ROUTE_APPLY_SHIELD_GRAPH
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply.applyShield
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.ApplyShieldSharedViewModel

private const val ROUTE_CHOOSE_PERSONAS = "choose_personas"
private const val ARG_MUST_SELECT_ONE = "arg_must_select_one"

internal class ChoosePersonasArgs(
    val mustSelectOne: Boolean
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        mustSelectOne = requireNotNull(savedStateHandle.get<Boolean>(ARG_MUST_SELECT_ONE))
    )
}

fun NavController.choosePersonas(mustSelectOne: Boolean) {
    navigate("$ROUTE_CHOOSE_PERSONAS/$mustSelectOne")
}

fun NavGraphBuilder.choosePersonas(
    navController: NavController
) {
    composable(
        route = "$ROUTE_CHOOSE_PERSONAS/{$ARG_MUST_SELECT_ONE}",
        arguments = listOf(
            navArgument(ARG_MUST_SELECT_ONE) { type = NavType.BoolType }
        ),
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
            onSelected = { address ->
                sharedVM.onPersonaSelected(address)
                navController.applyShield(sharedVM.args.securityStructureId)
            }
        )
    }
}
