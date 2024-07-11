package com.babylon.wallet.android.presentation.transfer

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.radixdlt.sargon.Account
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
internal const val ARG_ACCOUNT_ID = "arg_account_id"
const val ROUTE_TRANSFER = "transfer/{$ARG_ACCOUNT_ID}"

internal class TransferArgs(val accountId: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_ACCOUNT_ID]) as String)
    )
}

fun NavController.transfer(accountId: AccountAddress) {
    navigate("transfer/${accountId.string}")
}

fun NavGraphBuilder.transferScreen(
    onBackClick: () -> Unit,
    onAssetClicked: (SpendingAsset, Account) -> Unit
) {
    markAsHighPriority(ROUTE_TRANSFER)
    composable(
        route = ROUTE_TRANSFER,
        arguments = listOf(
            navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
        )
    ) {
        TransferScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onAssetClicked = onAssetClicked
        )
    }
}
