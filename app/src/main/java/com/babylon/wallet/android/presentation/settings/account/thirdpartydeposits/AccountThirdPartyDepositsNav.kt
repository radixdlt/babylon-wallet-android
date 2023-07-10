package com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_ADDRESS = "arg_address"

internal class AccountThirdPartyDepositsArgs(val address: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_ADDRESS]) as String)
}

fun NavController.accountThirdPartyDeposits(address: String) {
    navigate("account_third_party_deposits_route/$address")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountThirdPartyDeposits(onBackClick: () -> Unit, onAssetSpecificRulesClick: (String) -> Unit) {
    composable(
        route = "account_third_party_deposits_route/{$ARG_ADDRESS}",
        arguments = listOf(
            navArgument(ARG_ADDRESS) { type = NavType.StringType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        AccountThirdPartyDepositsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onAssetSpecificRulesClick = onAssetSpecificRulesClick
        )
    }
}
