package com.babylon.wallet.android.presentation.account.settings.delete.moveassets

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
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

private const val ROUTE = "deleting_account_move_assets"
private const val ARG_DELETING_ACCOUNT_ADDRESS = "deleting_account_address"

internal class DeletingAccountMoveAssetsArgs(val deletingAccountAddress: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_DELETING_ACCOUNT_ADDRESS]))
    )
}

fun NavController.deletingAccountMoveAssets(deletingAccountAddress: AccountAddress) {
    navigate(route = "$ROUTE?$ARG_DELETING_ACCOUNT_ADDRESS=${deletingAccountAddress.string}")
}

fun NavGraphBuilder.deletingAccountMoveAssets(
    onDismiss: () -> Unit
) {
    composable(
        route = "$ROUTE?$ARG_DELETING_ACCOUNT_ADDRESS={$ARG_DELETING_ACCOUNT_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_DELETING_ACCOUNT_ADDRESS) { type = NavType.StringType }
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
        DeletingAccountMoveAssetsScreen(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
