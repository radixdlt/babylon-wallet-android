package com.babylon.wallet.android.presentation.dapp.authorized.account

import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.Event
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.babylon.wallet.android.presentation.dapp.authorized.verifyentities.EntitiesForProofWithSignatures

@VisibleForTesting
internal const val ARG_AUTHORIZED_REQUEST_INTERACTION_ID = "arg_authorized_request_interaction_id"

@VisibleForTesting
internal const val ARG_NUMBER_OF_ACCOUNTS = "number_of_accounts"

@VisibleForTesting
internal const val ARG_IS_EXACT_ACCOUNT_COUNT = "exact_account_count"

@VisibleForTesting
internal const val ARG_IS_ONE_TIME_REQUEST = "one_time"

@VisibleForTesting
internal const val ARG_SHOW_BACK = "show_back"

internal const val ROUTE_CHOOSE_ACCOUNTS = "choose_accounts_route/" +
    "{$ARG_AUTHORIZED_REQUEST_INTERACTION_ID}/" +
    "{$ARG_NUMBER_OF_ACCOUNTS}/" +
    "{$ARG_IS_EXACT_ACCOUNT_COUNT}/" +
    "{$ARG_IS_ONE_TIME_REQUEST}/" +
    "{$ARG_SHOW_BACK}"

internal class ChooseAccountsArgs(
    val authorizedRequestInteractionId: String,
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean,
    val isOneTimeRequest: Boolean,
    val showBack: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_AUTHORIZED_REQUEST_INTERACTION_ID]) as String,
        checkNotNull(savedStateHandle[ARG_NUMBER_OF_ACCOUNTS]) as Int,
        checkNotNull(savedStateHandle[ARG_IS_EXACT_ACCOUNT_COUNT]) as Boolean,
        checkNotNull(savedStateHandle[ARG_IS_ONE_TIME_REQUEST]) as Boolean,
        checkNotNull(savedStateHandle[ARG_SHOW_BACK]) as Boolean
    )
}

fun NavController.chooseAccounts(
    authorizedRequestInteractionId: String,
    isOneTimeRequest: Boolean,
    isExactAccountsCount: Boolean,
    numberOfAccounts: Int,
    showBack: Boolean = false
) {
    navigate("choose_accounts_route/$authorizedRequestInteractionId/$numberOfAccounts/$isExactAccountsCount/$isOneTimeRequest/$showBack")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.chooseAccounts(
    onAccountCreationClick: () -> Unit,
    onNavigateToChooseAccounts: (Event.NavigateToChooseAccounts) -> Unit,
    onLoginFlowComplete: () -> Unit,
    onBackClick: () -> Boolean,
    onNavigateToOngoingPersonaData: (Event.NavigateToOngoingPersonaData) -> Unit,
    onNavigateToOneTimePersonaData: (Event.NavigateToOneTimePersonaData) -> Unit,
    onNavigateToVerifyPersona: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (interactionId: String, EntitiesForProofWithSignatures) -> Unit,
    navController: NavController,
) {
    composable(
        route = ROUTE_CHOOSE_ACCOUNTS,
        arguments = listOf(
            navArgument(ARG_AUTHORIZED_REQUEST_INTERACTION_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_NUMBER_OF_ACCOUNTS) {
                type = NavType.IntType
            },
            navArgument(ARG_IS_EXACT_ACCOUNT_COUNT) {
                type = NavType.BoolType
            },
            navArgument(ARG_IS_ONE_TIME_REQUEST) {
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
            onNavigateToChooseAccounts = onNavigateToChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            onBackClick = onBackClick,
            onNavigateToOngoingPersonaData = onNavigateToOngoingPersonaData,
            onNavigateToOneTimePersonaData = onNavigateToOneTimePersonaData,
            onNavigateToVerifyPersona = onNavigateToVerifyPersona,
            onNavigateToVerifyAccounts = onNavigateToVerifyAccounts
        )
    }
}

private fun requiresHorizontalTransition(arguments: Bundle?): Boolean {
    arguments ?: return false
    return arguments.getBoolean(ARG_SHOW_BACK)
}
