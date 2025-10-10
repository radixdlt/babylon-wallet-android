package com.babylon.wallet.android.presentation.timedrecovery

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val ARG_ACCOUNT_ADDRESS = "arg_account_address"
private const val DESTINATION = "route_account_timed_recovery/{$ARG_ACCOUNT_ADDRESS}"
private const val ROUTE = "$DESTINATION/{$ARG_ACCOUNT_ADDRESS}"

class TimedRecoveryArgs private constructor(
    val accountAddress: AccountAddress
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        accountAddress = AccountAddress.init(
            validatingAddress = requireNotNull(
                savedStateHandle.get<String>(
                    ARG_ACCOUNT_ADDRESS
                )
            )
        )
    )
}

fun NavController.timedRecovery(accountAddress: AccountAddress) {
    navigate(route = "$DESTINATION/${accountAddress.string}")
}

fun NavGraphBuilder.timedRecovery(
    onDismiss: () -> Unit
) {
    dialog(
        route = ROUTE,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        arguments = listOf(
            navArgument(ARG_ACCOUNT_ADDRESS) {
                type = NavType.StringType
            }
        )
    ) {
        AccountTimedRecoveryBottomSheet(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
