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
import com.babylon.wallet.android.presentation.accountpreference.accountPreferences
import com.babylon.wallet.android.presentation.accountpreference.accountPreferencesScreen
import com.babylon.wallet.android.presentation.createaccount.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.createaccount.ROUTE_CREATE_ACCOUNT
import com.babylon.wallet.android.presentation.createaccount.createAccountConfirmationScreen
import com.babylon.wallet.android.presentation.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.createpersona.ROUTE_CREATE_PERSONA
import com.babylon.wallet.android.presentation.createpersona.createPersonaConfirmationScreen
import com.babylon.wallet.android.presentation.createpersona.createPersonaScreen
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_ID
import com.babylon.wallet.android.presentation.navigation.Screen.Companion.ARG_ACCOUNT_NAME
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
                onAccountClick = { accountId, accountName ->
                    navController.navigate(
                        Screen.AccountDestination.routeWithArgs(accountId, accountName)
                    )
                },
                onAccountCreationClick = {
                    navController.createAccountScreen(CreateAccountRequestSource.AccountsList)
                }
            )
        }
        composable(
            route = Screen.AccountDestination.route + "/{$ARG_ACCOUNT_ID}/{$ARG_ACCOUNT_NAME}",
            arguments = listOf(
                navArgument(ARG_ACCOUNT_ID) { type = NavType.StringType },
                navArgument(ARG_ACCOUNT_NAME) { type = NavType.StringType },
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
        ) {
            AccountScreen(
                viewModel = hiltViewModel(),
                onAccountPreferenceClick = { address ->
                    navController.accountPreferences(address = address)
                },
                onBackClick = {
                    navController.navigateUp()
                }
            )
        }
        createAccountScreen(
            startDestination,
            onBackClick = {
                navController.navigateUp()
            },
            onContinueClick = { accountId, requestSource ->
                navController.createAccountConfirmationScreen(
                    accountId,
                    requestSource ?: CreateAccountRequestSource.FirstTime
                )
            }
        )
        createAccountConfirmationScreen(
            onNavigateToWallet = {
                navController.popBackStack(Screen.WalletDestination.route, inclusive = false)
            },
            onFinishAccountCreation = {
                navController.popBackStack(ROUTE_CREATE_ACCOUNT, inclusive = true)
            }
        )
        createPersonaScreen(
            onBackClick = { navController.navigateUp() },
            onContinueClick = { personaId ->
                navController.createPersonaConfirmationScreen(personaId = personaId)
            }
        )

        transactionApprovalScreen(onBackClick = {
            navController.popBackStack()
        })
        accountPreferencesScreen(onBackClick = {
            navController.popBackStack()
        })
        dAppRequestAccountsGraph(navController)
        settingsNavGraph(navController)
        createPersonaConfirmationScreen(
            finishPersonaCreation = {
                navController.popBackStack(ROUTE_CREATE_PERSONA, inclusive = true)
            }
        )
    }
}
