package com.babylon.wallet.android.presentation.settings.accountsecurity.seedphrases.reveal

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

@VisibleForTesting
internal const val ARG_FACTOR_SOURCE_ID = "factor_source_id"

const val ROUTE_REVEAL_SEED_PHRASE = "reveal_seed_phrase/{$ARG_FACTOR_SOURCE_ID}"

internal class RevealSeedPhraseArgs(val factorSourceId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_FACTOR_SOURCE_ID]) as String
    )
}

fun NavController.revealSeedPhrase(factorSourceId: String) {
    navigate("reveal_seed_phrase/$factorSourceId")
}

fun NavGraphBuilder.revealSeedPhrase(
    onBackClick: () -> Unit,
    onConfirmSeedPhraseClick: (String, Int) -> Unit
) {
    composable(
        route = ROUTE_REVEAL_SEED_PHRASE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        arguments = listOf(
            navArgument(ARG_FACTOR_SOURCE_ID) {
                type = NavType.StringType
            }
        )
    ) {
        RevealSeedPhraseScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onConfirmSeedPhraseClick = onConfirmSeedPhraseClick
        )
    }
}
