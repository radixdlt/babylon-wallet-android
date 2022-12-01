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
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.CreateAccountScreen
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_NAME
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_GRADIENT_INDEX
import com.babylon.wallet.android.presentation.navigation.dapp.dAppConnectionGraph
import com.babylon.wallet.android.presentation.onboarding.OnboardingScreen
import com.babylon.wallet.android.presentation.settings.SettingsScreen
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
                onMenuClick = {
                    navController.navigate(Screen.SettingsDestination.route)
                },
                onAccountClick = { accountId, accountName, gradientIndex ->
                    navController.navigate(
                        Screen.AccountDestination.routeWithArgs(accountId, accountName, gradientIndex)
                    )
                },
                onAccountCreationClick = {
                    navController.navigate(
                        Screen.CreateAccountDestination.route
                    )
                }
            )
        }
        composable(route = Screen.SettingsDestination.route) {
            SettingsScreen(
                viewModel = hiltViewModel()
            )
        }
        composable(
            route = Screen.AccountDestination.route + "/{$ARG_ACCOUNT_ID}/{$ARG_ACCOUNT_NAME}/{$ARG_GRADIENT_INDEX}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
                navArgument(ARG_ACCOUNT_NAME) { type = NavType.StringType },
                navArgument(ARG_GRADIENT_INDEX) { type = NavType.IntType }
            )
        ) { navBackStackEntry ->
            AccountScreen(
                viewModel = hiltViewModel(),
                accountName = navBackStackEntry.arguments?.getString(ARG_ACCOUNT_NAME).orEmpty(),
                onMenuItemClick = {
                    /* TODO For now i init flow here for testing */
                    navController.navigate(
                        Screen.DAppDestination.route
                    )
                }
            ) {
                navController.navigateUp()
            }
        }
        composable(route = Screen.CreateAccountDestination.route) {
            CreateAccountScreen(
                onBackClick = { navController.navigateUp() },
                onContinueClick = { accountId, accountName ->
                    navController.navigate(
                        Screen.AccountCompletionDestination.routeWithArgs(accountId, accountName)
                    )
                }
            )
        }
        composable(
            route = Screen.AccountCompletionDestination.route + "/{$ARG_ACCOUNT_ID}/{$ARG_ACCOUNT_NAME}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
                navArgument(ARG_ACCOUNT_NAME) { type = NavType.StringType }
            )
        ) {
            CreateAccountConfirmationScreen(
                viewModel = hiltViewModel(),
                goHomeClick = {
                    navController.popBackStack(Screen.WalletDestination.route, inclusive = false)
                }
            )
        }

        dAppConnectionGraph(navController)
    }
}
