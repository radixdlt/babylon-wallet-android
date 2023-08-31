package com.babylon.wallet.android.presentation.settings.accountsecurity

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.ledgerhardwarewallets.ledgerHardwareWalletsScreen
import com.babylon.wallet.android.presentation.settings.legacyimport.importLegacyWalletScreen
import com.babylon.wallet.android.presentation.settings.seedphrases.seedPhrases
import com.google.accompanist.navigation.animation.composable
import com.google.accompanist.navigation.animation.navigation

const val ROUTE_ACCOUNT_SECURITY_SCREEN = "settings_account_security_screen"
const val ROUTE_ACCOUNT_SECURITY_GRAPH = "settings_account_security_graph"

fun NavController.accountSecurityScreen() {
    navigate(ROUTE_ACCOUNT_SECURITY_SCREEN)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountSecurityNavGraph(
    navController: NavController,
) {
    navigation(
        startDestination = ROUTE_ACCOUNT_SECURITY_SCREEN,
        route = ROUTE_ACCOUNT_SECURITY_GRAPH
    ) {
        accountSecurityScreen(navController)
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountSecurityScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_ACCOUNT_SECURITY_SCREEN,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        AccountSecurityScreen(
            viewModel = hiltViewModel(),
            onAccountSecuritySettingItemClick = { accountSecurityAndSettingsItem ->
                when (accountSecurityAndSettingsItem) {
                    SettingsItem.AccountSecurityAndSettingsItem.SeedPhrases -> {
                        navController.seedPhrases()
                    }
                    SettingsItem.AccountSecurityAndSettingsItem.LedgerHardwareWallets -> {
                        navController.ledgerHardwareWalletsScreen()
                    }
                    SettingsItem.AccountSecurityAndSettingsItem.DepositGuarantees -> {
                    }
                    SettingsItem.AccountSecurityAndSettingsItem.ImportFromLegacyWallet -> {
                        navController.importLegacyWalletScreen()
                    }
                }
            },
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}
