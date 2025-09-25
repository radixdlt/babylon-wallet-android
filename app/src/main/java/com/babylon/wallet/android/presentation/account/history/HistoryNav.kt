package com.babylon.wallet.android.presentation.account.history

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
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
internal const val ARG_ACCOUNT_ADDRESS = "arg_account_address"
const val ROUTE_HISTORY = "history/{$ARG_ACCOUNT_ADDRESS}"

internal class HistoryArgs(val accountAddress: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_ACCOUNT_ADDRESS]))
    )
}

fun NavController.history(accountAddress: AccountAddress) {
    navigate("history/${accountAddress.string}")
}

fun NavGraphBuilder.history(
    onBackClick: () -> Unit,
) {
    markAsHighPriority(ROUTE_HISTORY)
    composable(
        route = ROUTE_HISTORY,
        arguments = listOf(
            navArgument(ARG_ACCOUNT_ADDRESS) { type = NavType.StringType },
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
        HistoryScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
