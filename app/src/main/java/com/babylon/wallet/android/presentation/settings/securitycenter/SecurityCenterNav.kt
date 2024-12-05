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
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.ledgerHardwareWalletsScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.securityFactors
import com.babylon.wallet.android.presentation.settings.securitycenter.securityshields.onboarding.securityShieldOnboardingScreen
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
                    navController.securityShieldOnboardingScreen()
                },
                onSecurityFactorsClick = {
                    navController.securityFactors()
                },
                onBackupConfigurationClick = {
                    navController.backupScreen()
                },
                onRecoverEntitiesClick = {
                    navController.restoreMnemonics(args = RestoreMnemonicsArgs(requestSource = RestoreMnemonicsRequestSource.Settings))
                },
                onBackupEntities = {
                    navController.seedPhrases()
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
        securityFactors(
            onBackClick = {
                navController.popBackStack()
            },
            onSecurityFactorSettingItemClick = { item ->
                when (item) {
                    is SettingsItem.SecurityFactorsSettingsItem.LedgerHardwareWallets -> {
                        navController.ledgerHardwareWalletsScreen()
                    }

                    is SettingsItem.SecurityFactorsSettingsItem.SeedPhrases -> {
                        navController.seedPhrases()
                    }
                }
            }
        )
        seedPhrases(
            onBackClick = { navController.popBackStack() },
            onNavigateToRecoverMnemonic = {
                navController.restoreMnemonics(args = RestoreMnemonicsArgs(requestSource = RestoreMnemonicsRequestSource.Settings))
            },
            onNavigateToSeedPhrase = { navController.revealSeedPhrase(it) }
        )
        ledgerHardwareWalletsScreen(
            onInfoClick = { glossaryItem ->
                navController.infoDialog(glossaryItem)
            },
            onBackClick = {
                navController.navigateUp()
            }
        )
        securityShieldsNavGraph(navController)
    }
}
