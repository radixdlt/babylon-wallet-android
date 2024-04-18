package com.babylon.wallet.android.presentation.settings.securitycenter

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.RestoreMnemonicsArgs
import com.babylon.wallet.android.presentation.onboarding.restore.mnemonics.restoreMnemonics
import com.babylon.wallet.android.presentation.settings.SettingsItem
import com.babylon.wallet.android.presentation.settings.securitycenter.ledgerhardwarewallets.ledgerHardwareWalletsScreen
import com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.securityFactors
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.reveal.revealSeedPhrase
import com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases.seedPhrases

const val ROUTE_SECURITY_CENTER_SCREEN = "settings_security_center_screen"
const val ROUTE_SECURITY_CENTER_GRAPH = "settings_security_center_graph"

fun NavController.securityCenter() {
    navigate(ROUTE_SECURITY_CENTER_SCREEN)
}

fun NavGraphBuilder.securityCenter(
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
                null
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
            },
            popEnterTransition = {
                EnterTransition.None
            }
        ) {
            SecurityCenterScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSecurityFactorsClick = {
                    navController.securityFactors()
                }
            )
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
                navController.restoreMnemonics(args = RestoreMnemonicsArgs())
            },
            onNavigateToSeedPhrase = { navController.revealSeedPhrase(it.body.value) }
        )
        ledgerHardwareWalletsScreen(
            onBackClick = {
                navController.navigateUp()
            }
        )
    }
}
