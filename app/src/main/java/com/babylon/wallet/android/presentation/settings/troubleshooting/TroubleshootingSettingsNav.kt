package com.babylon.wallet.android.presentation.settings.troubleshooting

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.account.createaccount.withledger.LedgerSelectionPurpose
import com.babylon.wallet.android.presentation.account.createaccount.withledger.chooseLedger
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonic.addSingleMnemonic
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.confirm.confirmSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.reveal.revealSeedPhrase
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.accountRecoveryScanSelection
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.chooseseed.chooseSeedPhrase
import com.babylon.wallet.android.presentation.settings.troubleshooting.accountrecoveryscan.scan.accountRecoveryScan
import com.babylon.wallet.android.presentation.settings.troubleshooting.importlegacywallet.importLegacyWalletScreen

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
            null
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
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
                    else -> {}
                }
            }
        )
        importLegacyWalletScreen(
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
            onRecoveryScanWithFactorSource = { factorSourceId, isOlympia ->
                navController.accountRecoveryScan(
                    factorSourceId = factorSourceId,
                    isOlympia = isOlympia
                )
            }
        )
    }
}
