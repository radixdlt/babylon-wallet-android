package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import rdx.works.profile.data.model.factorsources.FactorSource

const val ROUTE_SETTINGS_SHOW_MNEMONIC = "settings_seed_phrases"

fun NavController.seedPhrases() {
    navigate(ROUTE_SETTINGS_SHOW_MNEMONIC) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.seedPhrases(
    onBackClick: () -> Unit,
    onNavigateToRecoverMnemonic: () -> Unit,
    onNavigateToSeedPhrase: (FactorSource.FactorSourceID.FromHash) -> Unit
) {
    composable(
        route = ROUTE_SETTINGS_SHOW_MNEMONIC,
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
        SeedPhrasesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onNavigateToRecoverMnemonic = onNavigateToRecoverMnemonic,
            onNavigateToSeedPhrase = onNavigateToSeedPhrase
        )
    }
}
