package com.babylon.wallet.android.presentation.dapp.authorized.account

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginEvent
import com.babylon.wallet.android.presentation.dapp.authorized.login.DAppAuthorizedLoginViewModel
import com.babylon.wallet.android.presentation.dapp.authorized.login.ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH
import com.google.accompanist.navigation.animation.composable

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
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.chooseAccounts(
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (DAppAuthorizedLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (DAppAuthorizedLoginEvent.LoginFlowCompleted) -> Unit,
    onBackClick: () -> Boolean,
    onPersonaOngoingData: (DAppAuthorizedLoginEvent.PersonaDataOngoing) -> Unit,
    onPersonaDataOnetime: (DAppAuthorizedLoginEvent.PersonaDataOnetime) -> Unit,
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
        )
    ) {
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        ChooseAccountsScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedVM,
            dismissErrorDialog = dismissErrorDialog,
            onAccountCreationClick = onAccountCreationClick,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            onBackClick = onBackClick,
            onPersonaOngoingData = onPersonaOngoingData,
            onPersonaDataOnetime = onPersonaDataOnetime
        )
    }
}
