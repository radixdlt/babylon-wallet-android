package com.babylon.wallet.android.presentation.timedrecovery

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.radixdlt.sargon.AddressOfAccountOrPersona
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val ARG_ADDRESS = "arg_address"
private const val DESTINATION = "route_timed_recovery/{$ARG_ADDRESS}"
private const val ROUTE = "$DESTINATION/{$ARG_ADDRESS}"

class TimedRecoveryArgs private constructor(
    val address: AddressOfAccountOrPersona
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        address = AddressOfAccountOrPersona.init(
            validating = requireNotNull(
                savedStateHandle.get<String>(
                    ARG_ADDRESS
                )
            )
        )
    )
}

fun NavController.timedRecovery(address: AddressOfAccountOrPersona) {
    navigate(route = "$DESTINATION/${address.string}")
}

fun NavGraphBuilder.timedRecovery(
    onDismiss: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    dialog(
        route = ROUTE,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        arguments = listOf(
            navArgument(ARG_ADDRESS) {
                type = NavType.StringType
            }
        )
    ) {
        TimedRecoveryBottomSheet(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onInfoClick = onInfoClick
        )
    }
}
