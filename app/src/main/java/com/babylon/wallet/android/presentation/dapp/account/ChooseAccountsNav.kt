package com.babylon.wallet.android.presentation.dapp.account

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.InitialRoute
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
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
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onLoginFlowComplete: (String?) -> Unit,
    initialRoute: InitialRoute.ChooseAccount?,
    sharedViewModel: DAppLoginViewModel,
    onBackClick: () -> Boolean
) {
    composable(
        route = ROUTE_CHOOSE_ACCOUNTS,
        arguments = listOf(
            navArgument(ARG_NUMBER_OF_ACCOUNTS) {
                type = NavType.IntType
                initialRoute?.let {
                    defaultValue = initialRoute.numberOfAccounts
                }
            },
            navArgument(ARG_EXACT_ACCOUNT_COUNT) {
                type = NavType.BoolType
                initialRoute?.let {
                    defaultValue = initialRoute.isExactAccountsCount
                }
            },
            navArgument(ARG_ONE_TIME) {
                type = NavType.BoolType
                initialRoute?.let {
                    defaultValue = initialRoute.oneTime
                }
            },
            navArgument(ARG_SHOW_BACK) {
                type = NavType.BoolType
                initialRoute?.let {
                    defaultValue = initialRoute.showBack
                }
            },
        )
    ) {
        ChooseAccountsScreen(
            viewModel = hiltViewModel(),
            sharedViewModel = sharedViewModel,
            dismissErrorDialog = dismissErrorDialog,
            onAccountCreationClick = onAccountCreationClick,
            onChooseAccounts = onChooseAccounts,
            onLoginFlowComplete = onLoginFlowComplete,
            onBackClick = onBackClick
        )
    }
}
