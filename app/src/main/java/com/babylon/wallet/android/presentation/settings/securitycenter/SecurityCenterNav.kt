package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.main.MAIN_ROUTE
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsRequestSource
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.securitycenter.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.arculusCards
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.biometricsPin
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice.ledgerDevices
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.offdevicemnemonic.offDeviceMnemonics
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.password.passwords
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.securityFactorTypes
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding.securityShieldOnboarding
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.securityShieldsNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.securityShieldsScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.reveal.revealSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.seedPhrases

const val ROUTE_SECURITY_CENTER_SCREEN = "settings_security_center_screen"
const val ROUTE_SECURITY_CENTER_GRAPH = "settings_security_center_graph"

fun NavController.securityCenter() {
    navigate(ROUTE_SECURITY_CENTER_GRAPH)
}

@Suppress("LongMethod")
fun NavGraphBuilder.securityCenterNavGraph(
    navController: NavController
) {
    navigation(
        startDestination = ROUTE_SECURITY_CENTER_SCREEN,
        route = ROUTE_SECURITY_CENTER_GRAPH
    ) {
        composable(
            route = ROUTE_SECURITY_CENTER_SCREEN,
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
            SecurityCenterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                toSecurityShields = {
                    navController.securityShieldsScreen()
                },
                toSecurityShieldsOnboarding = {
                    navController.securityShieldOnboarding()
                },
                onSecurityFactorsClick = {
                    navController.securityFactorTypes()
                },
                onBackupConfigurationClick = {
                    navController.backupScreen()
                },
                onRecoverEntitiesClick = {
                    navController.restoreMnemonics(args = RestoreMnemonicsArgs(requestSource = RestoreMnemonicsRequestSource.Settings))
                },
                onBackupEntities = {
                    navController.biometricsPin()
                }
            )
        }
        backupScreen(
            onProfileDeleted = {
                navController.popBackStack(MAIN_ROUTE, false)
            }
        ) {
            navController.popBackStack()
        }
        securityFactorTypes(
            onBackClick = {
                navController.popBackStack()
            },
            onSecurityFactorTypeClick = { item ->
                when (item) {
                    is SettingsItem.SecurityFactorsSettingsItem.BiometricsPin -> {
                        navController.biometricsPin()
                    }

                    is SettingsItem.SecurityFactorsSettingsItem.LedgerNano -> {
                        navController.ledgerDevices()
                    }

                    SettingsItem.SecurityFactorsSettingsItem.ArculusCard -> {
                        navController.arculusCards()
                    }
                    SettingsItem.SecurityFactorsSettingsItem.OffDeviceMnemonic -> {
                        navController.offDeviceMnemonics()
                    }
                    SettingsItem.SecurityFactorsSettingsItem.Password -> {
                        navController.passwords()
                    }
                }
            }
        )
        biometricsPin(
            onNavigateToDeviceFactorSourceDetails = { }, // TODO next task
            onNavigateToAddBiometricPin = { }, // TODO next task
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBackClick = { navController.popBackStack() }
        )
        ledgerDevices(
            onNavigateToLedgerFactorSourceDetails = { }, // TODO next task
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBackClick = { navController.navigateUp() }
        )
        arculusCards(
            onNavigateToArculusFactorSourceDetails = { }, // TODO next task
            onNavigateToAddArculusCard = { },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBackClick = { navController.navigateUp() }
        )
        offDeviceMnemonics(
            onNavigateToOffDeviceMnemonicFactorSourceDetails = { }, // TODO next task
            onNavigateToOffDeviceAddMnemonic = { },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBackClick = { navController.navigateUp() }
        )
        passwords(
            onNavigateToPasswordFactorSourceDetails = { }, // TODO next task
            onNavigateToAddPassword = { },
            onInfoClick = { glossaryItem -> navController.infoDialog(glossaryItem) },
            onBackClick = { navController.navigateUp() }
        )
        seedPhrases( // TODO remove it later
            onBackClick = { navController.popBackStack() },
            onNavigateToRecoverMnemonic = {
                navController.restoreMnemonics(args = RestoreMnemonicsArgs(requestSource = RestoreMnemonicsRequestSource.Settings))
            },
            onNavigateToSeedPhrase = { navController.revealSeedPhrase(it) }
        )
        securityShieldsNavGraph(navController)
    }
}
