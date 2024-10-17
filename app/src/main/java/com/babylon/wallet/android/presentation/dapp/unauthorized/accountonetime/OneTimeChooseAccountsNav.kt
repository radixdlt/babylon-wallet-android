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
import com.babylon.wallet.android.presentation.dapp.unauthorized.verifyentities.EntitiesForProofWithSignatures

@VisibleForTesting
internal const val ARG_NUMBER_OF_ACCOUNTS = "arg_number_of_accounts"

@VisibleForTesting
internal const val ARG_UNAUTHORIZED_REQUEST_INTERACTION_ID = "arg_unauthorized_request_interaction_id"

@VisibleForTesting
internal const val ARG_EXACT_ACCOUNT_COUNT = "arg_exact_account_count"

private const val ROUTE_CHOOSE_ACCOUNTS_ONETIME = "choose_accounts_onetime_route/" +
    "{$ARG_UNAUTHORIZED_REQUEST_INTERACTION_ID}/" +
    "{$ARG_NUMBER_OF_ACCOUNTS}/" +
    "{$ARG_EXACT_ACCOUNT_COUNT}"

internal class OneTimeChooseAccountsArgs(
    val unauthorizedRequestInteractionId: String,
    val numberOfAccounts: Int,
    val isExactAccountsCount: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        unauthorizedRequestInteractionId = checkNotNull(savedStateHandle[ARG_UNAUTHORIZED_REQUEST_INTERACTION_ID]) as String,
        numberOfAccounts = checkNotNull(savedStateHandle[ARG_NUMBER_OF_ACCOUNTS]) as Int,
        isExactAccountsCount = checkNotNull(savedStateHandle[ARG_EXACT_ACCOUNT_COUNT]) as Boolean
    )
}

fun NavController.oneTimeChooseAccounts(
    unauthorizedRequestInteractionId: String,
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean
) {
    navigate("choose_accounts_onetime_route/$unauthorizedRequestInteractionId/$numberOfAccounts/$isExactAccountsCount")
}

@Suppress("LongParameterList")
fun NavGraphBuilder.oneTimeChooseAccounts(
    exitRequestFlow: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onLoginFlowComplete: () -> Unit,
    onNavigateToChoosePersonaOnetime: (RequiredPersonaFields) -> Unit,
    onNavigateToVerifyPersona: (String, EntitiesForProofWithSignatures) -> Unit,
    onNavigateToVerifyAccounts: (String, EntitiesForProofWithSignatures) -> Unit,
    navController: NavController
) {
    composable(
        route = ROUTE_CHOOSE_ACCOUNTS_ONETIME,
        arguments = listOf(
            navArgument(ARG_UNAUTHORIZED_REQUEST_INTERACTION_ID) {
                type = NavType.StringType
            },
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
            onNavigateToChoosePersonaOnetime = onNavigateToChoosePersonaOnetime,
            onNavigateToVerifyPersona = onNavigateToVerifyPersona,
            onNavigateToVerifyAccounts = onNavigateToVerifyAccounts
        )
    }
}
