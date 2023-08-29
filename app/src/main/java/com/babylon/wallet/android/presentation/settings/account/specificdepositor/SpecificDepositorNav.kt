package com.babylon.wallet.android.presentation.settings.account.specificdepositor

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits.AccountThirdPartyDepositsViewModel
import com.babylon.wallet.android.presentation.settings.account.thirdpartydeposits.ROUTE_ACCOUNT_THIRD_PARTY_DEPOSITS
import com.google.accompanist.navigation.animation.composable

fun NavController.specificDepositor() {
    navigate("account_specific_depositor_route")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.specificDepositor(navController: NavController, onBackClick: () -> Unit) {
    composable(
        route = "account_specific_depositor_route",
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
        SpecificDepositorScreen(
            sharedViewModel = sharedVM,
            onBackClick = onBackClick,
        )
    }
}
