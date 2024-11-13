package com.babylon.wallet.android.presentation.account.settings.delete

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
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val ROUTE = "delete_account"
private const val ARG_ACCOUNT_ADDRESS = "account_address"

internal class DeleteAccountArgs(val accountAddress: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        accountAddress = AccountAddress.init(requireNotNull(savedStateHandle[ARG_ACCOUNT_ADDRESS]))
    )
}

fun NavController.deleteAccount(accountAddress: AccountAddress) {
    navigate(route = "$ROUTE?${ARG_ACCOUNT_ADDRESS}=${accountAddress.string}")
}

fun NavGraphBuilder.deleteAccount(
    onMoveAssetsToAnotherAccount: (AccountAddress) -> Unit,
    onDismiss: () -> Unit
) {
    composable(
        route = "$ROUTE?$ARG_ACCOUNT_ADDRESS={$ARG_ACCOUNT_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ACCOUNT_ADDRESS) { type = NavType.StringType }
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
        DeleteAccountScreen(
            viewModel = hiltViewModel(),
            onMoveAssetsToAnotherAccount = onMoveAssetsToAnotherAccount,
            onDismiss = onDismiss
        )
    }
}
