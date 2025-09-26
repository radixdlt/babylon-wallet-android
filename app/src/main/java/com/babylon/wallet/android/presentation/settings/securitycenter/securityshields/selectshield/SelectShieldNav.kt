package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectshield

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
import com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.apply.applyShield
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.createSecurityShield
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val DESTINATION_SELECT_SHIELD = "select_shield"
private const val ARG_ENTITY_ADDRESS = "arg_entity_address"
private const val ROUTE_SELECT_SHIELD = "$DESTINATION_SELECT_SHIELD/{$ARG_ENTITY_ADDRESS}"

class ApplyShieldToEntityArgs(
    val address: AddressOfAccountOrPersona
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        address = AddressOfAccountOrPersona.init(
            requireNotNull(savedStateHandle.get<String>(ARG_ENTITY_ADDRESS))
        )
    )
}

fun NavController.applyShieldToEntity(address: AddressOfAccountOrPersona) {
    navigate("$DESTINATION_SELECT_SHIELD/${address.string}")
}

fun NavGraphBuilder.applyShieldToEntity(
    navController: NavController
) {
    composable(
        route = ROUTE_SELECT_SHIELD,
        arguments = listOf(
            navArgument(ARG_ENTITY_ADDRESS) { type = NavType.StringType }
        ),
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        ApplyShieldToEntityScreen(
            viewModel = hiltViewModel(),
            onCreateShieldClick = { address -> navController.createSecurityShield(address) },
            onDismiss = navController::popBackStack,
            onComplete = { securityStructureId, entityAddress ->
                navController.applyShield(securityStructureId, entityAddress) {
                    popUpTo(ROUTE_SELECT_SHIELD) {
                        inclusive = true
                    }
                }
            }
        )
    }
}
