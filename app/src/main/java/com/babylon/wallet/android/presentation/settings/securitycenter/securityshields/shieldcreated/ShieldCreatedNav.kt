package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shieldcreated

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.applyShieldNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.ROUTE_CREATE_SECURITY_SHIELD_GRAPH
import com.radixdlt.sargon.SecurityStructureId

private const val ROUTE_SHIELD_CREATED = "shield_created"
private const val ARG_SECURITY_STRUCTURE_ID = "arg_security_structure_id"

internal class ShieldCreatedArgs(
    val securityStructureId: SecurityStructureId
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        securityStructureId = SecurityStructureId.fromString(requireNotNull(savedStateHandle.get<String>(ARG_SECURITY_STRUCTURE_ID)))
    )
}

fun NavController.shieldCreated(id: SecurityStructureId) {
    navigate("$ROUTE_SHIELD_CREATED/$id")
}

fun NavGraphBuilder.shieldCreated(
    navController: NavController
) {
    composable(
        route = "$ROUTE_SHIELD_CREATED/{$ARG_SECURITY_STRUCTURE_ID}",
        arguments = listOf(
            navArgument(ARG_SECURITY_STRUCTURE_ID) {
                type = NavType.StringType
            }
        ),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        ShieldCreatedScreen(
            viewModel = hiltViewModel(),
            onDismiss = {
                navController.popBackStack(ROUTE_CREATE_SECURITY_SHIELD_GRAPH, false)
            },
            onApply = { id ->
                navController.applyShieldNavGraph(id) {
                    popUpTo(ROUTE_CREATE_SECURITY_SHIELD_GRAPH) { inclusive = false }
                }
            }
        )
    }
}
