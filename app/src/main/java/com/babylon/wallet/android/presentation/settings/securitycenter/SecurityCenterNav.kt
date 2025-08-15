package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.addfactorsource.addFactorSource
import com.babylon.wallet.android.presentation.addfactorsource.kind.addFactorSourceKind
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.ImportMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.ImportMnemonicsRequestSource
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.importMnemonics
import com.babylon.wallet.android.presentation.selectfactorsource.selectFactorSource
import com.babylon.wallet.android.presentation.settings.securitycenter.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.arculusCards
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.changepin.changeArculusPin
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.verifypin.verifyArculusPin
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.biometricsPin
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.seedphrase.confirm.confirmSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.seedphrase.reveal.ROUTE_REVEAL_SEED_PHRASE
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.seedphrase.reveal.revealSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails.ROUTE_FACTOR_SOURCE_DETAILS
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails.factorSourceDetails
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.ledgerdevice.ledgerDevices
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.offdevicemnemonic.offDeviceMnemonics
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.password.passwords
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.securityFactorTypes
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding.securityShieldOnboarding
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.securityShieldsNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.securityShieldsScreen
import com.radixdlt.sargon.FactorSourceKind

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
                    navController.importMnemonics(args = ImportMnemonicsArgs(requestSource = ImportMnemonicsRequestSource.Settings))
                },
                onBackupEntities = {
                    navController.biometricsPin()
                }
            )
        }
        backupScreen(
            onClose = {
                navController.popBackStack()
            }
        )
        securityFactorTypes(
            onBackClick = {
                navController.popBackStack()
            },
            onSecurityFactorTypeClick = { kind ->
                when (kind) {
                    FactorSourceKind.DEVICE -> navController.biometricsPin()
                    FactorSourceKind.LEDGER_HQ_HARDWARE_WALLET -> navController.ledgerDevices()
                    FactorSourceKind.OFF_DEVICE_MNEMONIC -> navController.offDeviceMnemonics()
                    FactorSourceKind.ARCULUS_CARD -> navController.arculusCards()
                    FactorSourceKind.PASSWORD -> navController.passwords()
                }
            }
        )
        biometricsPin(
            onNavigateToDeviceFactorSourceDetails = { navController.factorSourceDetails(it) },
            onNavigateToWriteDownSeedPhrase = { navController.revealSeedPhrase(it) },
            onNavigateToSeedPhraseRestore = {
                navController.importMnemonics(
                    args = ImportMnemonicsArgs(
                        requestSource = ImportMnemonicsRequestSource.FactorSourceDetails
                    )
                )
            },
            onBackClick = { navController.popBackStack() }
        )
        ledgerDevices(
            toFactorSourceDetails = { navController.factorSourceDetails(it) },
            onBackClick = { navController.navigateUp() }
        )
        arculusCards(
            onNavigateToArculusFactorSourceDetails = { navController.factorSourceDetails(it) },
            onBackClick = { navController.navigateUp() }
        )
        verifyArculusPin(
            onDismiss = navController::popBackStack,
            onComplete = { factorSourceId, pin ->
                navController.changeArculusPin(factorSourceId, pin) {
                    popUpTo(route = ROUTE_FACTOR_SOURCE_DETAILS) {
                        inclusive = false
                    }
                }
            }
        )
        changeArculusPin(
            onDismiss = navController::popBackStack,
            onComplete = navController::popBackStack
        )
        offDeviceMnemonics(
            onNavigateToOffDeviceMnemonicFactorSourceDetails = { navController.factorSourceDetails(it) },
            onBackClick = { navController.navigateUp() }
        )
        passwords(
            onNavigateToPasswordFactorSourceDetails = { navController.factorSourceDetails(it) },
            onBackClick = { navController.navigateUp() }
        )
        factorSourceDetails(
            navigateToViewSeedPhrase = { factorSourceId ->
                navController.revealSeedPhrase(factorSourceId = factorSourceId)
            },
            navigateToViewSeedPhraseRestore = {
                navController.importMnemonics(
                    args = ImportMnemonicsArgs(
                        requestSource = ImportMnemonicsRequestSource.FactorSourceDetails
                    )
                )
            },
            toChangeArculusPin = navController::verifyArculusPin,
            toForgotArculusPin = {
                // TODO sergiu
            },
            onBackClick = { navController.navigateUp() }
        )
        revealSeedPhrase(
            onBackClick = {
                navController.navigateUp()
            },
            onConfirmSeedPhraseClick = { factorSourceId, mnemonicSize ->
                navController.confirmSeedPhrase(factorSourceId, mnemonicSize)
            }
        )
        confirmSeedPhrase(
            onMnemonicBackedUp = {
                navController.popBackStack(ROUTE_REVEAL_SEED_PHRASE, inclusive = true)
            },
            onDismiss = {
                navController.popBackStack()
            }
        )
        securityShieldsNavGraph(navController)
        addFactorSourceKind(navController)
        addFactorSource(navController)
        selectFactorSource(
            onDismiss = navController::popBackStack,
            onComplete = { navController.popBackStack() }
        )
    }
}
