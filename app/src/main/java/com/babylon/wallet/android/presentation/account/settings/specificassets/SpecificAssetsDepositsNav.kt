package com.babylon.wallet.android.presentation.account.settings.specificassets

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AccountThirdPartyDepositsViewModel
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS

fun NavController.specificAssets() {
    navigate("account_specific_assets_route")
}

fun NavGraphBuilder.specificAssets(navController: NavController, onBackClick: () -> Unit) {
    composable(
        route = "account_specific_assets_route",
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
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS)
        }
        val sharedVM = hiltViewModel<AccountThirdPartyDepositsViewModel>(parentEntry)
        SpecificAssetsDepositsScreen(
            sharedViewModel = sharedVM,
            onBackClick = onBackClick,
        )
    }
}
