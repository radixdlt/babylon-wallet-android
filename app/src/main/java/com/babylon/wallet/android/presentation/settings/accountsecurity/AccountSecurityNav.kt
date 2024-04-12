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
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.addSingleMnemonic
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountrecoveryscan.accountRecoveryScanSelection
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountrecoveryscan.chooseseed.chooseSeedPhrase
import com.babylon.wallet.android.presentation.settings.accountsecurity.accountrecoveryscan.scan.accountRecoveryScan
import com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees.depositGuaranteesScreen
import com.babylon.wallet.android.presentation.settings.accountsecurity.importlegacywallet.importLegacyWalletScreen
import com.babylon.wallet.android.presentation.settings.appsettings.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.appsettings.backup.systemBackupSettingsScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.confirm.confirmSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.reveal.revealSeedPhrase

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
                navController.accountRecoveryScan(
                    factorSourceId = factorSource.identifier,
                    isOlympia = isOlympia
                )
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
