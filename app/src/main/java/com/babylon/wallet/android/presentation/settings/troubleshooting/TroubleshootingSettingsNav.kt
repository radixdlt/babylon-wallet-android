package com.babylon.wallet.android.presentation.settings.troubleshooting

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.account.createaccount.withledger.LedgerSelectionPurpose
import com.babylon.wallet.android.presentation.account.createaccount.withledger.chooseLedger
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.addSingleMnemonic
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.accountRecoveryScanSelection
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.chooseseed.chooseSeedPhrase
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan.accountRecoveryScan
import com.babylon.wallet.android.presentation.settings.troubleshooting.importlegacywallet.importLegacyWalletScreen
import com.babylon.wallet.android.presentation.settings.troubleshooting.reset.resetWalletScreen

const val ROUTE_TROUBLESHOOTING_GRAPH = "settings_troubleshooting_graph"
const val ROUTE_TROUBLESHOOTING_SCREEN = "settings_troubleshooting_screen"

fun NavController.troubleshootingSettings() {
    navigate(ROUTE_TROUBLESHOOTING_GRAPH)
}

fun NavGraphBuilder.troubleshootingSettings(onBackClick: () -> Unit, onSettingItemClick: (SettingsItem.Troubleshooting) -> Unit) {
    composable(
        route = ROUTE_TROUBLESHOOTING_SCREEN,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        TroubleshootingSettingsScreen(
            onBackClick = onBackClick,
            onSettingItemClick = onSettingItemClick
        )
    }
}

@Suppress("LongMethod")
fun NavGraphBuilder.troubleshootingNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_TROUBLESHOOTING_SCREEN,
        route = ROUTE_TROUBLESHOOTING_GRAPH
    ) {
        troubleshootingSettings(
            onBackClick = {
                navController.popBackStack()
            },
            onSettingItemClick = { item ->
                when (item) {
                    SettingsItem.Troubleshooting.AccountRecovery -> navController.accountRecoveryScanSelection()
                    SettingsItem.Troubleshooting.ImportFromLegacyWallet -> navController.importLegacyWalletScreen()
                    SettingsItem.Troubleshooting.FactoryReset -> navController.resetWalletScreen()
                    else -> {}
                }
            }
        )
        importLegacyWalletScreen(
            onInfoClick = { glossaryItem ->
                navController.infoDialog(glossaryItem)
            },
            onBackClick = {
                navController.popBackStack()
            }
        )
        resetWalletScreen(
            onProfileDeleted = {
                navController.popBackStack(MAIN_ROUTE, false)
            },
            onBackClick = {
                navController.popBackStack()
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
            onRecoveryScanWithFactorSource = { factorSourceId, isOlympia ->
                navController.accountRecoveryScan(
                    factorSourceId = factorSourceId,
                    isOlympia = isOlympia
                )
            }
        )
    }
}
