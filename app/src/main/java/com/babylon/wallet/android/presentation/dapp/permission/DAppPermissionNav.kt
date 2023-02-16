package com.babylon.wallet.android.presentation.dapp.permission

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
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

const val ROUTE_DAPP_PERMISSION =
    "dapp_permission/{$ARG_NUMBER_OF_ACCOUNTS}/{$ARG_EXACT_ACCOUNT_COUNT}/{$ARG_ONE_TIME}"

fun NavController.dappPermission(
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    oneTime: Boolean = false
) {
    navigate("dapp_permission/$numberOfAccounts/$isExactAccountsCount/$oneTime")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.dappPermission(
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onCompleteFlow: () -> Unit,
    initialRoute: InitialRoute.Permission?,
    sharedViewModel: DAppLoginViewModel,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_DAPP_PERMISSION,
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
        )
    ) { entry ->
        val numberOfAccounts = checkNotNull(entry.arguments?.getInt(ARG_NUMBER_OF_ACCOUNTS))
        val quantifier = checkNotNull(entry.arguments?.getBoolean(ARG_EXACT_ACCOUNT_COUNT))
        DAppPermissionScreen(
            viewModel = sharedViewModel,
            onChooseAccounts = onChooseAccounts,
            numberOfAccounts = numberOfAccounts,
            isExactAccountsCount = quantifier,
            onCompleteFlow = onCompleteFlow,
            onBackClick = onBackClick
        )
    }
}
