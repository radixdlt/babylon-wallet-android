package com.babylon.wallet.android.presentation.dapp.permission

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.dapp.InitialDappLoginRoute
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

// NOT USED
fun NavController.loginPermission(
    numberOfAccounts: Int,
    isExactAccountsCount: Boolean,
    oneTime: Boolean
) {
    navigate("dapp_permission/$numberOfAccounts/$isExactAccountsCount/$oneTime")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.loginPermission(
    onChooseAccounts: (DAppLoginEvent.ChooseAccounts) -> Unit,
    onCompleteFlow: () -> Unit,
    initialDappLoginRoute: InitialDappLoginRoute.Permission?,
    sharedViewModel: DAppLoginViewModel,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_DAPP_PERMISSION,
        arguments = listOf(
            navArgument(ARG_NUMBER_OF_ACCOUNTS) {
                type = NavType.IntType
                initialDappLoginRoute?.let {
                    defaultValue = initialDappLoginRoute.numberOfAccounts
                }
            },
            navArgument(ARG_EXACT_ACCOUNT_COUNT) {
                type = NavType.BoolType
                initialDappLoginRoute?.let {
                    defaultValue = initialDappLoginRoute.isExactAccountsCount
                }
            },
            navArgument(ARG_ONE_TIME) {
                type = NavType.BoolType
                initialDappLoginRoute?.let {
                    defaultValue = initialDappLoginRoute.oneTime
                }
            }
        )
    ) { entry ->
        val numberOfAccounts = checkNotNull(entry.arguments?.getInt(ARG_NUMBER_OF_ACCOUNTS))
        val quantifier = checkNotNull(entry.arguments?.getBoolean(ARG_EXACT_ACCOUNT_COUNT))
        val oneTime = checkNotNull(entry.arguments?.getBoolean(ARG_ONE_TIME))
        LoginPermissionScreen(
            viewModel = sharedViewModel,
            onChooseAccounts = onChooseAccounts,
            numberOfAccounts = numberOfAccounts,
            isExactAccountsCount = quantifier,
            onCompleteFlow = onCompleteFlow,
            onBackClick = onBackClick,
            oneTime = oneTime
        )
    }
}
