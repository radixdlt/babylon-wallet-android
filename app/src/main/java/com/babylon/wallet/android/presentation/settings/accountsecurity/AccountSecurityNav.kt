package com.babylon.wallet.android.presentation.settings.accountsecurity

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees.depositGuaranteesScreen
import com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet.importLegacyWalletScreen
import com.babylon.wallet.android.presentation.settings.accountsecurity.ledgerhardwarewallets.ledgerHardwareWalletsScreen
import com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.confirm.confirmSeedPhrase
import com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.reveal.revealSeedPhrase
import com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.seedPhrases
import com.babylon.wallet.android.presentation.settings.appsettings.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.appsettings.backup.systemBackupSettingsScreen

const val ROUTE_ACCOUNT_SECURITY_SCREEN = "settings_account_security_screen"
const val ROUTE_ACCOUNT_SECURITY_GRAPH = "settings_account_security_graph"

fun NavController.accountSecurityScreen() {
    navigate(ROUTE_ACCOUNT_SECURITY_SCREEN)
}

fun NavGraphBuilder.accountSecurityNavGraph(
    navController: NavController,
) {
    navigation(
        startDestination = ROUTE_ACCOUNT_SECURITY_SCREEN,
        route = ROUTE_ACCOUNT_SECURITY_GRAPH
    ) {
        accountSecurityScreen(navController)
        seedPhrases(
            onBackClick = { navController.popBackStack() },
            onNavigateToRecoverMnemonic = {
                navController.restoreMnemonics(args = RestoreMnemonicsArgs.RestoreProfile())
            },
            onNavigateToSeedPhrase = { navController.revealSeedPhrase(it.body.value) }
        )
        ledgerHardwareWalletsScreen(
            onBackClick = {
                navController.navigateUp()
            }
        )
        importLegacyWalletScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
        backupScreen(
            onSystemBackupSettingsClick = {
                navController.systemBackupSettingsScreen()
            },
            onProfileDeleted = {
                navController.popBackStack(MAIN_ROUTE, false)
            },
            onClose = {
                navController.popBackStack()
            }
        )
        depositGuaranteesScreen(
            onBackClick = {
                navController.popBackStack()
            }
        )
        revealSeedPhrase(
            onBackClick = {
                navController.navigateUp()
            },
            onConfirmSeedPhraseClick = { factorSourceId, mnemonicSize ->
                navController.confirmSeedPhrase(factorSourceId, mnemonicSize)
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.accountSecurityScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_ACCOUNT_SECURITY_SCREEN,
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
                        navController.depositGuaranteesScreen()
                    }
                    is SettingsItem.AccountSecurityAndSettingsItem.Backups -> {
                        navController.backupScreen()
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
