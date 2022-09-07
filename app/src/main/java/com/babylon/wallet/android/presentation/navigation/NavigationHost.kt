package com.babylon.wallet.android.presentation.navigation

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.account.AccountScreen
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_NAME
import com.babylon.wallet.android.presentation.onboarding.CreateAccountScreen
import com.babylon.wallet.android.presentation.onboarding.OnboardingScreen
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import com.google.accompanist.pager.ExperimentalPagerApi

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationHost(
    startDestination: String
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(route = Screen.OnboardingDestination.route) {
            OnboardingScreen(
                viewModel = hiltViewModel(),
                restoreWalletFromBackup = {}
            )
        }
        composable(route = Screen.WalletDestination.route) {
            WalletScreen(
                viewModel = hiltViewModel(),
                onAccountClick = { accountId, accountName ->
                    navController.navigate(
                        Screen.AccountDestination.routeWithArgs(accountId, accountName)
                    )
                },
                onAccountCreationClick = {
                    navController.navigate(
                        Screen.CreateAccountDestination.route
                    )
                }
            )
        }
        composable(
            route = Screen.AccountDestination.route + "/{$ARG_ACCOUNT_ID}/{$ARG_ACCOUNT_NAME}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
                navArgument(ARG_ACCOUNT_NAME) { type = NavType.StringType }
            )
        ) { navBackStackEntry ->
            AccountScreen(
                viewModel = hiltViewModel(),
                accountName = navBackStackEntry.arguments?.getString(ARG_ACCOUNT_NAME).orEmpty(),
                onMenuItemClick = { }
            ) {
                navController.navigateUp()
            }
        }
        composable(route = Screen.CreateAccountDestination.route) {
            CreateAccountScreen(
                onBackClick = { navController.navigateUp() },
                onContinueClick = { /* TODO */ }
            )
        }
    }
}
