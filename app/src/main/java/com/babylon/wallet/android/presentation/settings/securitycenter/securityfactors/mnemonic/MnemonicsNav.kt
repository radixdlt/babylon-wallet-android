package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.mnemonic

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.radixdlt.sargon.FactorSourceId

private const val ROUTE_MNEMONICS_SCREEN = "route_mnemonics_screen"

fun NavController.mnemonics() {
    navigate(ROUTE_MNEMONICS_SCREEN) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.mnemonics(
    onNavigateToMnemonicFactorSourceDetails: (factorSourceId: FactorSourceId) -> Unit,
    onNavigateToAddMnemonic: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_MNEMONICS_SCREEN,
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
        MnemonicsScreen(
            viewModel = hiltViewModel(),
            onNavigateToMnemonicFactorSourceDetails = onNavigateToMnemonicFactorSourceDetails,
            onNavigateToAddMnemonic = onNavigateToAddMnemonic,
            onInfoClick = onInfoClick,
            onBackClick = onBackClick
        )
    }
}
