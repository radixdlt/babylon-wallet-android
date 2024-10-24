package com.babylon.wallet.android.presentation.dapp.unauthorized.accountonetime

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
import com.babylon.wallet.android.domain.model.messages.RequiredPersonaFields
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.DAppUnauthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.unauthorized.login.ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH

@VisibleForTesting
internal const val ARG_NUMBER_OF_ACCOUNTS = "number_of_accounts"

@VisibleForTesting
internal const val ARG_EXACT_ACCOUNT_COUNT = "exact_account_count"

internal const val ROUTE_CHOOSE_ACCOUNTS_ONETIME = "choose_accounts_onetime_route/{$ARG_NUMBER_OF_ACCOUNTS}/{$ARG_EXACT_ACCOUNT_COUNT}"

internal class OneTimeChooseAccountsArgs(
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_NUMBER_OF_ACCOUNTS]) as Int,
        checkNotNull(savedStateHandle[ARG_EXACT_ACCOUNT_COUNT]) as Boolean
    )
}

fun NavController.chooseAccountsOneTime(numberOfAccounts: Int, isExactAccountsCount: Boolean) {
    navigate("choose_accounts_onetime_route/$numberOfAccounts/$isExactAccountsCount")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.chooseAccountsOneTime(
    exitRequestFlow: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    onPersonaOnetime: (RequiredPersonaFields) -> Unit,
    navController: NavController
) {
    composable(
        route = ROUTE_CHOOSE_ACCOUNTS_ONETIME,
        arguments = listOf(
            navArgument(ARG_NUMBER_OF_ACCOUNTS) {
                type = NavType.IntType
            },
            navArgument(ARG_EXACT_ACCOUNT_COUNT) {
                type = NavType.BoolType
            }
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
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_UNAUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppUnauthorizedLoginViewModel>(parentEntry)
        OneTimeChooseAccountsScreen(
            viewModel = hiltViewModel(),
            exitRequestFlow = exitRequestFlow,
            onAccountCreationClick = onAccountCreationClick,
            sharedViewModel = sharedVM,
            onLoginFlowComplete = onLoginFlowComplete,
            onPersonaOnetime = onPersonaOnetime
        )
    }
}
