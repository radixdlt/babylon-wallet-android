package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.addfactorsource.addFactorSource
import com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin.CreateArculusPinContext
import com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin.createArculusPin
import com.babylon.wallet.android.presentation.addfactorsource.kind.addFactorSourceKind
import com.babylon.wallet.android.presentation.addfactorsource.name.setFactorSourceName
import com.babylon.wallet.android.presentation.dialogs.address.addressDetails
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.ImportMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.ImportMnemonicsRequestSource
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.importMnemonics
import com.babylon.wallet.android.presentation.selectfactorsource.selectFactorSource
import com.babylon.wallet.android.presentation.settings.securitycenter.backup.backupScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.arculusCards
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.changepin.changeArculusPin
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.forgotpin.forgotArculusPin
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
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.createSecurityShield
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.createSecurityShieldNavGraph
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.selectshield.applyShieldToEntity
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shielddetails.securityShieldDetails
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.shields.securityShieldsScreen
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
                toCreateSecurityShield = {
                    navController.createSecurityShield()
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
        forgotArculusPin(
            onDismiss = navController::popBackStack,
            onComplete = { navController.createArculusPin(CreateArculusPinContext.Restore) }
        )
        createArculusPin(
            onDismiss = { navController.popBackStack() },
            onConfirmed = { context ->
                when (context) {
                    CreateArculusPinContext.New -> navController.setFactorSourceName()
                    CreateArculusPinContext.Restore -> navController.popBackStack(
                        route = ROUTE_FACTOR_SOURCE_DETAILS,
                        inclusive = false
                    )
                }
            }
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
            toForgotArculusPin = navController::forgotArculusPin,
            toAddress = navController::addressDetails,
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
        securityShieldsScreen(navController)
        securityShieldDetails(navController)
        createSecurityShieldNavGraph(navController)
        applyShieldToEntity(navController)
        addFactorSourceKind(navController)
        addFactorSource(navController)
        selectFactorSource(
            onDismiss = navController::popBackStack,
            onComplete = { navController.popBackStack() }
        )
    }
}
