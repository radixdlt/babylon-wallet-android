package com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits

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
import com.radixdlt.sargon.AccountAddress
import com.radixdlt.sargon.extensions.init
import com.radixdlt.sargon.extensions.string

@VisibleForTesting
internal const val ARG_ADDRESS = "arg_address"

const val ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS =
    "account_third_party_deposits_route/{$ARG_ADDRESS}"

internal class AccountThirdPartyDepositsArgs(val address: AccountAddress) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        AccountAddress.init(checkNotNull(savedStateHandle[ARG_ADDRESS]) as String)
    )
}

fun NavController.accountThirdPartyDeposits(address: AccountAddress) {
    navigate("account_third_party_deposits_route/${address.string}")
}

fun NavGraphBuilder.accountThirdPartyDeposits(
    navController: NavController,
    onBackClick: () -> Unit,
    onAssetSpecificRulesClick: (AccountAddress) -> Unit,
    onSpecificDepositorsClick: () -> Unit
) {
    composable(
        route = ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS,
        arguments = listOf(
            navArgument(ARG_ADDRESS) { type = NavType.StringType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        val backstackEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS)
        }
        val viewModel = hiltViewModel<AccountThirdPartyDepositsViewModel>(backstackEntry)
        AccountThirdPartyDepositsScreen(
            viewModel = viewModel,
            onBackClick = onBackClick,
            onAssetSpecificRulesClick = onAssetSpecificRulesClick,
            onSpecificDepositorsClick = onSpecificDepositorsClick
        )
    }
}
