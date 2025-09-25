package com.babylon.wallet.android.presentation.transfer

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
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
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
    onShowAssetDetails: (SpendingAsset, Account) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    markAsHighPriority(ROUTE_TRANSFER)
    composable(
        route = ROUTE_TRANSFER,
        arguments = listOf(
            navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        TransferScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onShowAssetDetails = onShowAssetDetails,
            onInfoClick = onInfoClick
        )
    }
}
