package com.babylon.wallet.android.presentation.account.settings.delete.success

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

private const val ROUTE = "deleted_account_success"
private const val ARG_DELETED_ACCOUNT_ADDRESS = "deleted_account_address"

internal class DeletedAccountSuccessArgs(val deletedAccountAddress: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle): this(
        deletedAccountAddress = AccountAddress.init(checkNotNull(savedStateHandle[ARG_DELETED_ACCOUNT_ADDRESS]))
    )
}

fun NavController.deletedAccountSuccess(deletedAccountAddress: AccountAddress) {
    navigate(route = "$ROUTE?$ARG_DELETED_ACCOUNT_ADDRESS=${deletedAccountAddress.string}")
}

fun NavGraphBuilder.deletedAccountSuccess(
    onGotoHomescreen: () -> Unit
) {
    composable(
        route = "$ROUTE?$ARG_DELETED_ACCOUNT_ADDRESS={$ARG_DELETED_ACCOUNT_ADDRESS}",
        arguments = listOf(navArgument(ARG_DELETED_ACCOUNT_ADDRESS) { type = NavType.StringType }),
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
        DeleteAccountSuccessScreen(
            viewModel = hiltViewModel(),
            onGotoHomescreen = onGotoHomescreen
        )
    }
}

