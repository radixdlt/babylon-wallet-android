package com.babylon.wallet.android.presentation.dapp.account

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.domain.model.MessageFromDataChannel
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.babylon.wallet.android.presentation.dapp.login.ROUTE_DAPP_LOGIN
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_NUMBER_OF_ACCOUNTS = "number_of_accounts"

@VisibleForTesting
internal const val ARG_ACCOUNT_QUANTIFIER = "account_quantifier"

@VisibleForTesting
internal const val ARG_ONE_TIME = "one_time"

internal class ChooseAccountsArgs(
    val numberOfAccounts: Int,
    val accountQuantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
    val oneTime: Boolean
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_NUMBER_OF_ACCOUNTS]) as Int,
        checkNotNull(savedStateHandle[ARG_ACCOUNT_QUANTIFIER])
            as MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
        checkNotNull(savedStateHandle[ARG_ONE_TIME]) as Boolean
    )
}

fun NavController.chooseAccounts(
    numberOfAccounts: Int,
    quantifier: MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier,
    oneTime: Boolean = false
) {
    navigate("choose_accounts_route/$numberOfAccounts/$quantifier/$oneTime")
}

@Suppress("LongParameterList")
@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.chooseAccounts(
    navController: NavController,
    onBackClick: () -> Unit,
    dismissErrorDialog: () -> Unit,
    onAccountCreationClick: () -> Unit,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String) -> Unit
) {
    composable(
        route = "choose_accounts_route/{$ARG_NUMBER_OF_ACCOUNTS}/{$ARG_ACCOUNT_QUANTIFIER}/{$ARG_ONE_TIME}",
        arguments = listOf(
            navArgument(ARG_NUMBER_OF_ACCOUNTS) { type = NavType.IntType },
            navArgument(ARG_ACCOUNT_QUANTIFIER) {
                type = NavType.EnumType(MessageFromDataChannel.IncomingRequest.AccountNumberQuantifier::class.java)
            },
            navArgument(ARG_ONE_TIME) { type = NavType.BoolType },
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN)
        }
        val sharedVm = hiltViewModel<DAppLoginViewModel>(parentEntry)
        ChooseAccountsScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedVm,
            onBackClick = onBackClick,
            dismissErrorDialog = dismissErrorDialog,
            onAccountCreationClick = onAccountCreationClick,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete
        )
    }
}
