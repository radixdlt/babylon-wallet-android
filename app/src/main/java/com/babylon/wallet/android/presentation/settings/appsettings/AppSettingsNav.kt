package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.account.createaccount.confirmation.CreateAccountRequestSource
import com.babylon.wallet.android.presentation.account.createaccount.createAccountScreen
import com.babylon.wallet.android.presentation.navigation.Screen
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees.depositGuaranteesScreen
import com.babylon.wallet.android.presentation.settings.appsettings.entityhiding.hiddenEntitiesScreen
import com.babylon.wallet.android.presentation.settings.appsettings.gateways.GatewaysScreen

const val ROUTE_WALLET_PREFERENCES_SCREEN = "settings_wallet_preferences_screen"
const val ROUTE_WALLET_PREFERENCES_GRAPH = "settings_wallet_preferences_graph"

fun NavController.walletPreferencesScreen() {
    navigate(ROUTE_WALLET_PREFERENCES_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.appSettingsNavGraph(
    navController: NavController,
) {
    navigation(
        startDestination = ROUTE_WALLET_PREFERENCES_SCREEN,
        route = ROUTE_WALLET_PREFERENCES_GRAPH
    ) {
        walletPreferencesScreen(navController)
        settingsGateway(navController)
        hiddenEntitiesScreen(onBackClick = {
            navController.popBackStack()
        })
        depositGuaranteesScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
    }
}

fun NavGraphBuilder.walletPreferencesScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_WALLET_PREFERENCES_SCREEN,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        WalletPreferencesScreen(
            viewModel = hiltViewModel(),
            onWalletPreferenceItemClick = { appSettingsItem ->
                when (appSettingsItem) {
                    SettingsItem.WalletPreferencesSettingsItem.Gateways -> {
                        navController.navigate(Screen.SettingsEditGatewayApiDestination.route)
                    }
                    is SettingsItem.WalletPreferencesSettingsItem.CrashReporting,
                    is SettingsItem.WalletPreferencesSettingsItem.DeveloperMode -> {}
                    SettingsItem.WalletPreferencesSettingsItem.EntityHiding -> {
                        navController.hiddenEntitiesScreen()
                    }

                    SettingsItem.WalletPreferencesSettingsItem.DepositGuarantees -> {
                        navController.depositGuaranteesScreen()
                    }
                }
            },
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}

private fun NavGraphBuilder.settingsGateway(navController: NavController) {
    composable(
        route = Screen.SettingsEditGatewayApiDestination.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        GatewaysScreen(
            viewModel = hiltViewModel(),
            onBackClick = {
                navController.popBackStack()
            },
            onCreateProfile = { url, networkId ->
                navController.createAccountScreen(
                    CreateAccountRequestSource.Gateways,
                    url,
                    networkId,
                    true
                )
            }
        )
    }
}
