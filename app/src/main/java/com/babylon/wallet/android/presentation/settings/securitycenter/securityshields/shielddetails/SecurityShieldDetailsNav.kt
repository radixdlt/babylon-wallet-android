package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails.factorSourceDetails
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.regularaccess.regularAccess
import com.radixdlt.sargon.SecurityStructureId

private const val ROUTE_SECURITY_SHIELD_DETAILS_SCREEN = "security_shield_details_screen"
private const val ARG_SECURITY_STRUCTURE_ID = "arg_security_structure_id"

internal class SecurityShieldDetailsArgs(
    val securityStructureId: SecurityStructureId
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        securityStructureId = SecurityStructureId.fromString(
            requireNotNull(savedStateHandle.get<String>(ARG_SECURITY_STRUCTURE_ID))
        )
    )
}

fun NavController.securityShieldDetails(
    securityStructureId: SecurityStructureId
) {
    navigate("$ROUTE_SECURITY_SHIELD_DETAILS_SCREEN/$securityStructureId")
}

fun NavGraphBuilder.securityShieldDetails(navController: NavController) {
    composable(
        route = "$ROUTE_SECURITY_SHIELD_DETAILS_SCREEN/{$ARG_SECURITY_STRUCTURE_ID}",
        arguments = listOf(
            navArgument(ARG_SECURITY_STRUCTURE_ID) {
                type = NavType.StringType
            }
        ),
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
        SecurityShieldDetailsScreen(
            viewModel = hiltViewModel(),
            onBackClick = { navController.navigateUp() },
            onFactorClick = { navController.factorSourceDetails(it) },
            onEditShield = { navController.regularAccess() }
        )
    }
}
