package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.factorsourcedetails

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

internal const val ARG_FACTOR_SOURCE_ID = "arg_factor_source_id"

private const val ROUTE_FACTOR_SOURCE_DETAILS = "route_factor_source_details/{$ARG_FACTOR_SOURCE_ID}"

internal class FactorSourceDetailsArgs(val factorSourceId: FactorSourceId) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        FactorSourceId.fromJson(checkNotNull(savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID)))
    )
}

fun NavController.factorSourceDetails(factorSourceId: FactorSourceId) {
    navigate("route_factor_source_details/${factorSourceId.toJson()}")
}

fun NavGraphBuilder.factorSourceDetails(
    navigateToViewSeedPhrase: (factorSourceId: FactorSourceId.Hash) -> Unit,
    navigateToViewSeedPhraseRestore: () -> Unit,
    onBackClick: () -> Unit,
) {
    composable(
        route = ROUTE_FACTOR_SOURCE_DETAILS,
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
        FactorSourceDetailsScreen(
            viewModel = hiltViewModel(),
            navigateToViewSeedPhrase = navigateToViewSeedPhrase,
            navigateToViewSeedPhraseRestore = navigateToViewSeedPhraseRestore,
            onBackClick = onBackClick
        )
    }
}
