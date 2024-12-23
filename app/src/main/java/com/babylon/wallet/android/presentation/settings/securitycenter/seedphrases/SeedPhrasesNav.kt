
package com.babylon.wallet.android.presentation.settings.securitycenter.seedphrases

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.FactorSourceId

@Deprecated("remove it when new design/flow is complete")
const val ROUTE_SETTINGS_SHOW_MNEMONIC = "settings_seed_phrases"

fun NavController.seedPhrases() {
    navigate(ROUTE_SETTINGS_SHOW_MNEMONIC) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.seedPhrases(
    onBackClick: () -> Unit,
    onNavigateToRecoverMnemonic: () -> Unit,
    onNavigateToSeedPhrase: (FactorSourceId.Hash) -> Unit
) {
    composable(
        route = ROUTE_SETTINGS_SHOW_MNEMONIC,
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
        SeedPhrasesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onNavigateToRecoverMnemonic = onNavigateToRecoverMnemonic,
            onNavigateToSeedPhrase = onNavigateToSeedPhrase
        )
    }
}
