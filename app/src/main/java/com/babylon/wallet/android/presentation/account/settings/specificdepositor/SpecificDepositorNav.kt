package com.babylon.wallet.android.presentation.account.settings.specificdepositor

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.AccountThirdPartyDepositsViewModel
import com.babylon.wallet.android.presentation.account.settings.thirdpartydeposits.ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS

fun NavController.specificDepositor() {
    navigate("account_specific_depositor_route")
}

fun NavGraphBuilder.specificDepositor(navController: NavController, onBackClick: () -> Unit) {
    composable(
        route = "account_specific_depositor_route",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        val parentEntry = remember(it) {
            navController.getBackStackEntry(ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS)
        }
        val sharedVM = hiltViewModel<AccountThirdPartyDepositsViewModel>(parentEntry)
        SpecificDepositorScreen(
            sharedViewModel = sharedVM,
            onBackClick = onBackClick,
        )
    }
}
