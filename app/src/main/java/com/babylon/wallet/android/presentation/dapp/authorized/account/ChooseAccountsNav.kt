package com.babylon.wallet.android.presentation.dapp.authorized.account

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
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

internal const val ROUTE_CHOOSE_ACCOUNTS =
    "choose_accounts_route/{$ARG_NUMBER_OF_ACCOUNTS}/{$ARG_EXACT_ACCOUNT_COUNT}/{$ARG_ONE_TIME}/{$ARG_SHOW_BACK}"

internal class ChooseAccountsArgs(
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val oneTime: Boolean,
    val showBack: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_NUMBER_OF_ACCOUNTS]) as Int,
        checkNotNull(savedStateHandle[ARG_EXACT_ACCOUNT_COUNT]) as Boolean,
        checkNotNull(savedStateHandle[ARG_ONE_TIME]) as Boolean,
        checkNotNull(savedStateHandle[ARG_SHOW_BACK]) as Boolean
    )
}

fun NavController.chooseAccounts(
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    oneTime: Boolean = false,
    showBack: Boolean = false
) {
    navigate("choose_accounts_route/$numberOfAccounts/$isExactAccountsCount/$oneTime/$showBack")
}

@Suppress("LongParameterList", "MagicNumber")
fun NavGraphBuilder.chooseAccounts(
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    onBackClick: () -> Boolean,
    onPersonaOngoingData: (Event.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (Event.PersonaDataOnetime) -> Unit,
    navController: NavController,
) {
    composable(
        route = ROUTE_CHOOSE_ACCOUNTS,
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
            },
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
    ) {
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        ChooseAccountsScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedVM,
            onAccountCreationClick = onAccountCreationClick,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            onBackClick = onBackClick,
            onPersonaOngoingData = onPersonaOngoingData,
            onPersonaDataOnetime = onPersonaDataOnetime
        )
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    return arguments.getBoolean(ARG_SHOW_BACK)
}
