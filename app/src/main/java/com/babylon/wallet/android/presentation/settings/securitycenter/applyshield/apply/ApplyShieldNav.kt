package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ARG_ENTITY_ADDRESS
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ARG_SECURITY_STRUCTURE_ID
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ApplyShieldArgs
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.ROUTE_APPLY_SHIELD_GRAPH
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.common.ApplyShieldSharedViewModel
import com.babylon.wallet.android.presentation.ui.composables.BackIconType
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.SecurityStructureId
import com.radixdlt.sargon.extensions.string

private const val DESTINATION_APPLY_SHIELD = "apply_shield"
private const val ROUTE_APPLY_SHIELD = DESTINATION_APPLY_SHIELD +
    "?$ARG_SECURITY_STRUCTURE_ID={$ARG_SECURITY_STRUCTURE_ID}" +
    "&$ARG_ENTITY_ADDRESS={$ARG_ENTITY_ADDRESS}"

fun NavController.applyShield(
    securityStructureId: SecurityStructureId,
    address: AddressOfAccountOrPersona? = null
) {
    navigate(
        DESTINATION_APPLY_SHIELD +
            "?$ARG_SECURITY_STRUCTURE_ID=$securityStructureId" +
            "&$ARG_ENTITY_ADDRESS=${address?.string}"
    )
}

fun NavGraphBuilder.applyShield(
    navController: NavController
) {
    composable(
        route = ROUTE_APPLY_SHIELD,
        arguments = listOf(
            navArgument(ARG_SECURITY_STRUCTURE_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_ENTITY_ADDRESS) {
                type = NavType.StringType
                nullable = true
            }
        ),
        enterTransition = {
            val isInEntityContext = targetState.arguments?.let {
                ApplyShieldArgs(it)
            }?.isInEntityContext ?: false

            if (isInEntityContext) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            }
        },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = {
            val isInEntityContext = initialState.arguments?.let {
                ApplyShieldArgs(it)
            }?.isInEntityContext ?: false

            if (isInEntityContext) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            }
        }
    ) {
        val parentEntry = remember(it) { navController.getBackStackEntry(ROUTE_APPLY_SHIELD_GRAPH) }
        val sharedVM = hiltViewModel<ApplyShieldSharedViewModel>(parentEntry)

        ApplyShieldScreen(
            viewModel = hiltViewModel(),
            backIconType = if (sharedVM.args.isInEntityContext) {
                BackIconType.Close
            } else {
                BackIconType.Back
            },
            entityAddress = requireNotNull(sharedVM.selectedAddress),
            onDismiss = { navController.popBackStack() },
            onShieldApplied = { navController.popBackStack(ROUTE_APPLY_SHIELD_GRAPH, true) }
        )
    }
}
