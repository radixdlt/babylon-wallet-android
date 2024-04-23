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
import com.radixdlt.sargon.Exactly32Bytes
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.FactorSourceIdFromHash
import com.radixdlt.sargon.FactorSourceKind
import com.radixdlt.sargon.extensions.asGeneral
import com.radixdlt.sargon.extensions.hex
import com.radixdlt.sargon.extensions.hexToBagOfBytes
import com.radixdlt.sargon.extensions.init

@VisibleForTesting
internal const val ARG_FACTOR_SOURCE_ID_BODY_HEX = "factor_source_id"

const val ROUTE_REVEAL_SEED_PHRASE = "reveal_seed_phrase/{$ARG_FACTOR_SOURCE_ID_BODY_HEX}"

internal class RevealSeedPhraseArgs(val factorSourceId: FactorSourceId.Hash) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        FactorSourceIdFromHash(
            kind = FactorSourceKind.DEVICE,
            body = Exactly32Bytes.init(checkNotNull(savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID_BODY_HEX)).hexToBagOfBytes())
        ).asGeneral()
    )
}

fun NavController.revealSeedPhrase(factorSourceId: FactorSourceId.Hash) {
    navigate("reveal_seed_phrase/${factorSourceId.value.body.hex}")
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
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        arguments = listOf(
            navArgument(ARG_FACTOR_SOURCE_ID_BODY_HEX) {
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
