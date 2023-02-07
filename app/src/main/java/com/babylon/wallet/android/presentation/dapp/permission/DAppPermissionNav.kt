package com.babylon.wallet.android.presentation.dapp.permission

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginEvent
import com.babylon.wallet.android.presentation.dapp.login.DAppLoginViewModel
import com.babylon.wallet.android.presentation.dapp.login.ROUTE_DAPP_LOGIN
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_NUMBER_OF_ACCOUNTS = "number_of_accounts"

@VisibleForTesting
internal const val ARG_EXACT_ACCOUNT_COUNT = "exact_account_count"

@VisibleForTesting
internal const val ARG_ONE_TIME = "one_time"

fun NavController.dappPermission(
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    oneTime: Boolean = false
) {
    navigate("dapp_permission/$numberOfAccounts/$isExactAccountsCount/$oneTime")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.dappPermission(
    navController: NavController,
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onCompleteFlow: () -> Unit
) {
    composable(
        route = "dapp_permission/{$ARG_NUMBER_OF_ACCOUNTS}/{$ARG_EXACT_ACCOUNT_COUNT}/{$ARG_ONE_TIME}",
        arguments = listOf(
            navArgument(ARG_NUMBER_OF_ACCOUNTS) { type = NavType.IntType },
            navArgument(ARG_EXACT_ACCOUNT_COUNT) { type = NavType.BoolType },
            navArgument(ARG_ONE_TIME) { type = NavType.BoolType },
        )
    ) { entry ->
        val parentEntry = remember(entry) {
            navController.getBackStackEntry(ROUTE_DAPP_LOGIN)
        }
        val vm = hiltViewModel<DAppLoginViewModel>(parentEntry)
        val numberOfAccounts = checkNotNull(entry.arguments?.getInt(ARG_NUMBER_OF_ACCOUNTS))
        val quantifier = checkNotNull(entry.arguments?.getBoolean(ARG_EXACT_ACCOUNT_COUNT))
        DAppPermissionScreen(
            viewModel = vm,
            onChooseAccounts = onChooseAccounts,
            numberOfAccounts = numberOfAccounts,
            isExactAccountsCount = quantifier,
            onCompleteFlow = onCompleteFlow
        )
    }
}
