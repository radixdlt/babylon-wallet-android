package com.babylon.wallet.android.presentation.settings.accountsecurity

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.account.createaccount.withledger.LedgerSelectionPurpose
import com.babylon.wallet.android.presentation.account.createaccount.withledger.chooseLedger
import com.babylon.wallet.android.presentation.account.recover.scan.accountRecoveryScan
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.addSingleMnemonic
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
import com.babylon.wallet.android.presentation.settings.recovery.accountRecoveryScanSelection
import com.babylon.wallet.android.presentation.settings.recovery.chooseseed.chooseSeedPhrase

const val ROUTE_ACCOUNT_SECURITY_SCREEN = "settings_account_security_screen"
const val ROUTE_ACCOUNT_SECURITY_GRAPH = "settings_account_security_graph"

fun NavController.accountSecurityScreen() {
    navigate(ROUTE_ACCOUNT_SECURITY_SCREEN)
}

@Suppress("LongMethod")
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
                navController.restoreMnemonics(args = RestoreMnemonicsArgs())
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
        accountRecoveryScanSelection(
            onBack = {
                navController.popBackStack()
            },
            onChooseSeedPhrase = {
                navController.chooseSeedPhrase(it)
            },
            onChooseLedger = { isOlympia ->
                navController.chooseLedger(
                    ledgerSelectionPurpose = if (isOlympia) {
                        LedgerSelectionPurpose.RecoveryScanOlympia
                    } else {
                        LedgerSelectionPurpose.RecoveryScanBabylon
                    }
                )
            }
        )
        chooseSeedPhrase(
            onBack = {
                navController.popBackStack()
            },
            onAddSeedPhrase = {
                navController.addSingleMnemonic(mnemonicType = it)
            },
            onRecoveryScanWithFactorSource = { factorSource, isOlympia ->
                navController.accountRecoveryScan(factorSource.identifier, isOlympia)
            }
        )
    }
}

fun NavGraphBuilder.accountSecurityScreen(
    navController: NavController
) {
    composable(
        route = ROUTE_ACCOUNT_SECURITY_SCREEN,
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

                    SettingsItem.AccountSecurityAndSettingsItem.AccountRecovery -> {
                        navController.accountRecoveryScanSelection()
                    }
                }
            },
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}
