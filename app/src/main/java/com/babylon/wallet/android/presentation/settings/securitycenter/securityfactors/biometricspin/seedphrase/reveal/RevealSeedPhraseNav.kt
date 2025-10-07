package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.biometricspin.seedphrase.reveal

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

@VisibleForTesting
internal const val ARG_FACTOR_SOURCE_ID = "factor_source_id"

const val ROUTE_REVEAL_SEED_PHRASE = "reveal_seed_phrase/{$ARG_FACTOR_SOURCE_ID}"

internal class RevealSeedPhraseArgs(val factorSourceId: FactorSourceId.Hash) {

    private constructor(factorSourceId: FactorSourceId) : this(factorSourceId as FactorSourceId.Hash)

    constructor(savedStateHandle: SavedStateHandle) : this(
        FactorSourceId.fromJson(checkNotNull(savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID)))
    )
}

fun NavController.revealSeedPhrase(factorSourceId: FactorSourceId) {
    navigate("reveal_seed_phrase/${factorSourceId.toJson()}")
}

fun NavGraphBuilder.revealSeedPhrase(
    onBackClick: () -> Unit,
    onConfirmSeedPhraseClick: (FactorSourceId.Hash, Int) -> Unit
) {
    composable(
        route = ROUTE_REVEAL_SEED_PHRASE,
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
