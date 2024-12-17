package com.babylon.wallet.android.presentation.dapp.authorized.ongoingaccounts

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH

@VisibleForTesting
internal const val ARG_NUMBER_OF_ACCOUNTS = "number_of_accounts"

@VisibleForTesting
internal const val ARG_EXACT_ACCOUNT_COUNT = "exact_account_count"

@VisibleForTesting
internal const val ARG_ONE_TIME = "one_time"

@VisibleForTesting
internal const val ARG_SHOW_BACK = "show_back"

const val ROUTE_DAPP_PERMISSION =
    "dapp_permission/{$ARG_NUMBER_OF_ACCOUNTS}/{$ARG_EXACT_ACCOUNT_COUNT}/{$ARG_ONE_TIME}/{$ARG_SHOW_BACK}"

fun NavController.ongoingAccounts(
    isOneTimeRequest: Boolean,
    isExactAccountsCount: Boolean,
    numberOfAccounts: Int,
    showBack: Boolean
) {
    navigate("dapp_permission/$numberOfAccounts/$isExactAccountsCount/$isOneTimeRequest/$showBack")
}

fun NavGraphBuilder.ongoingAccounts(
    navController: NavController,
    onChooseAccounts: (Event.NavigateToChooseAccounts) -> Unit,
    onCompleteFlow: () -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_DAPP_PERMISSION,
        arguments = listOf(
            navArgument(ARG_NUMBER_OF_ACCOUNTS) {
                type = NavType.IntType
            },
            navArgument(ARG_EXACT_ACCOUNT_COUNT) {
                type = NavType.BoolType
            },
            navArgument(ARG_ONE_TIME) {
                type = NavType.BoolType
            },
            navArgument(ARG_SHOW_BACK) {
                type = NavType.BoolType
            }
        ),
        enterTransition = {
            if (requiresHorizontalTransition(targetState.arguments)) {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
            } else {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
            }
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            if (requiresHorizontalTransition(initialState.arguments)) {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            } else {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
            }
        }
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        val numberOfAccounts = checkNotNull(entry.arguments?.getInt(ARG_NUMBER_OF_ACCOUNTS))
        val quantifier = checkNotNull(entry.arguments?.getBoolean(ARG_EXACT_ACCOUNT_COUNT))
        val oneTime = checkNotNull(entry.arguments?.getBoolean(ARG_ONE_TIME))
        val showBack = checkNotNull(entry.arguments?.getBoolean(ARG_SHOW_BACK))
        OngoingAccountsScreen(
            viewModel = sharedVM,
            onChooseAccounts = onChooseAccounts,
            numberOfAccounts = numberOfAccounts,
            isExactAccountsCount = quantifier,
            onCompleteFlow = onCompleteFlow,
            onBackClick = onBackClick,
            isOneTimeRequest = oneTime,
            showBack = showBack
        )
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    return arguments.getBoolean(ARG_SHOW_BACK)
}
