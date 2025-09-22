package com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.applyshield

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val ROUTE_APPLY_SHIELD_TO_ENTITY = "apply_shield_to_entity"
private const val ARG_ENTITY_ADDRESS = "arg_entity_address"

internal class ApplyShieldToEntityArgs(
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
    dialog(
        route = "$ROUTE_APPLY_SHIELD_TO_ENTITY/{$ARG_ENTITY_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ENTITY_ADDRESS) { type = NavType.StringType }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ApplyShieldToEntityScreen(
            viewModel = hiltViewModel(),
            onDismiss = navController::popBackStack,
            onComplete = navController::popBackStack
        )
    }
}
