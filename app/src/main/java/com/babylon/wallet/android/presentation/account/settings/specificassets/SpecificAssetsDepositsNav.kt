package com.babylon.wallet.android.presentation.account.settings.specificassets

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AccountThirdPartyDepositsViewModel
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS
import com.google.accompanist.navigation.animation.composable

fun NavController.specificAssets() {
    navigate("account_specific_assets_route")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.specificAssets(navController: NavController, onBackClick: () -> Unit) {
    composable(
        route = "account_specific_assets_route",
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
