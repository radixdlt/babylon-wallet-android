package com.babylon.wallet.android.presentation.dapp.authorized.permission

import androidx.annotation.VisibleForTesting
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

const val ROUTE_DAPP_PERMISSION =
    "dapp_permission/{$ARG_NUMBER_OF_ACCOUNTS}/{$ARG_EXACT_ACCOUNT_COUNT}/{$ARG_ONE_TIME}"

fun NavController.loginPermission(
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    oneTime: Boolean
) {
    navigate("dapp_permission/$numberOfAccounts/$isExactAccountsCount/$oneTime")
}

fun NavGraphBuilder.loginPermission(
    navController: NavController,
    onChooseAccounts: (Event.ChooseAccounts) -> Unit,
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
            }
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN_AUTHORIZED_GRAPH)
        }
        val sharedVM = hiltViewModel<DAppAuthorizedLoginViewModel>(parentEntry)
        val numberOfAccounts = checkNotNull(entry.arguments?.getInt(ARG_NUMBER_OF_ACCOUNTS))
        val quantifier = checkNotNull(entry.arguments?.getBoolean(ARG_EXACT_ACCOUNT_COUNT))
        val oneTime = checkNotNull(entry.arguments?.getBoolean(ARG_ONE_TIME))
        LoginPermissionScreen(
            viewModel = sharedVM,
            onChooseAccounts = onChooseAccounts,
            numberOfAccounts = numberOfAccounts,
            isExactAccountsCount = quantifier,
            onCompleteFlow = onCompleteFlow,
            onBackClick = onBackClick,
            oneTime = oneTime
        )
    }
}
