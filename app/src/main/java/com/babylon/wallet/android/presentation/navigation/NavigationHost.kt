package com.babylon.wallet.android.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.account.AccountScreen
import com.babylon.wallet.android.presentation.accountpreference.accountPreference
import com.babylon.wallet.android.presentation.accountpreference.accountPreferenceScreen
import com.babylon.wallet.android.presentation.createaccount.CreateAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.CreateAccountScreen
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_NAME
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_GRADIENT_INDEX
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_HAS_PROFILE
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_NETWORK_NAME
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_NETWORK_URL
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_SWITCH_NETWORK
import com.babylon.wallet.android.presentation.navigation.dapp.dAppRequestAccountsGraph
import com.babylon.wallet.android.presentation.navigation.settings.settingsNavGraph
import com.babylon.wallet.android.presentation.onboarding.OnboardingScreen
import com.babylon.wallet.android.presentation.transaction.transactionApprovalScreen
import com.babylon.wallet.android.presentation.wallet.WalletScreen
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.pager.ExperimentalPagerApi

@ExperimentalPagerApi
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationHost(
    startDestination: String,
    navController: NavHostController,
) {
    AnimatedNavHost(
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
                    navController.navigate(Screen.SettingsAllDestination.route)
                },
                onAccountClick = { accountId, accountName, gradientIndex ->
                    navController.navigate(
                        Screen.AccountDestination.routeWithArgs(accountId, accountName, gradientIndex)
                    )
                },
                onAccountCreationClick = {
                    navController.navigate(
                        Screen.CreateAccountDestination.route()
                    )
                }
            )
        }
        composable(
            route = Screen.AccountDestination.route + "/{$ARG_ACCOUNT_ID}/{$ARG_ACCOUNT_NAME}/{$ARG_GRADIENT_INDEX}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
                navArgument(ARG_ACCOUNT_NAME) { type = NavType.StringType },
                navArgument(ARG_GRADIENT_INDEX) { type = NavType.IntType }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
            },
            popEnterTransition = {
                EnterTransition.None
            },
            popExitTransition = {
                ExitTransition.None
            }
        ) { navBackStackEntry ->
            AccountScreen(
                viewModel = hiltViewModel(),
                accountName = navBackStackEntry.arguments?.getString(ARG_ACCOUNT_NAME).orEmpty(),
                onAccountPreferenceClick = { address ->
                    navController.accountPreference(address = address)
                }
            ) {
                navController.navigateUp()
            }
        }
        composable(
            route = Screen.CreateAccountDestination.route(),
            arguments = listOf(
                navArgument(ARG_NETWORK_URL) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(ARG_NETWORK_NAME) {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument(ARG_SWITCH_NETWORK) {
                    type = NavType.BoolType
                    defaultValue = false
                }
            ),
            enterTransition = {
                slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
            }
        ) {
            CreateAccountScreen(
                viewModel = hiltViewModel(),
                onBackClick = { navController.navigateUp() },
                cancelable = startDestination != Screen.CreateAccountDestination.route(),
                onContinueClick = { accountId, accountName, hasProfile ->
                    navController.navigate(
                        Screen.AccountCompletionDestination.routeWithArgs(accountId, accountName, hasProfile)
                    )
                }
            )
        }
        transactionApprovalScreen(onBackClick = {
            navController.popBackStack()
        })
        accountPreferenceScreen(onBackClick = {
            navController.popBackStack()
        })
        composable(
            route = Screen.AccountCompletionDestination.route +
                "/{$ARG_ACCOUNT_ID}/{$ARG_ACCOUNT_NAME}/{$ARG_HAS_PROFILE}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
                navArgument(ARG_ACCOUNT_NAME) { type = NavType.StringType },
                navArgument(ARG_HAS_PROFILE) { type = NavType.BoolType }
            )
        ) {
            CreateAccountConfirmationScreen(
                viewModel = hiltViewModel(),
                navigateToWallet = {
                    navController.popBackStack(Screen.WalletDestination.route, inclusive = false)
                },
                finishAccountCreation = {
                    navController.popBackStack(Screen.CreateAccountDestination.route(), inclusive = true)
                }
            )
        }
        dAppRequestAccountsGraph(navController)
        settingsNavGraph(navController)
    }
}
