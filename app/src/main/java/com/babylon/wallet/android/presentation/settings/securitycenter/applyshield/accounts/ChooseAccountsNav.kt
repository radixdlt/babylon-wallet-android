package com.babylon.wallet.android.presentation.settings.securitycenter.applyshield.accounts

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE_CHOOSE_ACCOUNTS = "choose_accounts"

fun NavController.chooseAccounts() {
    navigate(ROUTE_CHOOSE_ACCOUNTS)
}

fun NavGraphBuilder.chooseAccounts(
    navController: NavController
) {
    composable(
        route = ROUTE_CHOOSE_ACCOUNTS,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        ChooseAccountsScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() }
        )
    }
}
