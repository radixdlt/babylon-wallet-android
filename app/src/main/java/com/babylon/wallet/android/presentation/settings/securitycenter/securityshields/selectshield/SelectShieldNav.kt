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

private const val ROUTE_APPLY_SHIELD_TO_ENTITY = "apply_shield_to_entity"
private const val ARG_ENTITY_ADDRESS = "arg_entity_address"

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
    navigate("$ROUTE_APPLY_SHIELD_TO_ENTITY/${address.string}")
}

fun NavGraphBuilder.applyShieldToEntity(
    navController: NavController
) {
    composable(
        route = "$ROUTE_APPLY_SHIELD_TO_ENTITY/{$ARG_ENTITY_ADDRESS}",
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
            onCreateShieldClick = { navController.createSecurityShield(it) },
            onDismiss = navController::popBackStack,
            onComplete = { securityStructureId, entityAddress ->
                navController.applyShield(securityStructureId, entityAddress)
            }
        )
    }
}
