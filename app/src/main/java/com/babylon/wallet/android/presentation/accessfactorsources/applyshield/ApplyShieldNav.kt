package com.babylon.wallet.android.presentation.accessfactorsources.applyshield

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val ROUTE = "apply_shield_sheet/"
private const val ARG_ADDRESS = "arg_entity_address"

data class ApplyShieldArgs(
    val address: AddressOfAccountOrPersona
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        requireNotNull(savedStateHandle.get<String>(ARG_ADDRESS)).let {
            AddressOfAccountOrPersona.init(it)
        }
    )
}

fun NavController.applyShield(address: AddressOfAccountOrPersona) {
    navigate(route = "$ROUTE/${address.string}")
}

fun NavGraphBuilder.applyShieldDialog(
    onDismiss: () -> Unit
) {
    dialog(
        route = "$ROUTE/{$ARG_ADDRESS}",
        dialogProperties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        ApplyShieldDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
