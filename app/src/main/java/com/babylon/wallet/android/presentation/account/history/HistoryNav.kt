package com.babylon.wallet.android.presentation.account.history

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import org.jetbrains.annotations.VisibleForTesting

@VisibleForTesting
internal const val ARG_ACCOUNT_ADDRESS = "arg_account_address"
const val ROUTE_HISTORY = "history/{$ARG_ACCOUNT_ADDRESS}"

internal class HistoryArgs(val accountAddress: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_ACCOUNT_ADDRESS]) as String
    )
}

fun NavController.history(accountAddress: String) {
    navigate("history/$accountAddress")
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
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        },
        popEnterTransition = {
            null
        }
    ) {
        HistoryScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
