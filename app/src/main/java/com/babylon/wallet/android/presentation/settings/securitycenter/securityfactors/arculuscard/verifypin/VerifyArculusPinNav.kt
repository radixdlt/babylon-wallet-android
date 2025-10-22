package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.verifypin

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

private const val ARG_FACTOR_SOURCE_ID = "factor_source_id"
private const val PATH_VERIFY_ARCULUS_PIN_SCREEN = "route_verify_arculus_pin_screen"
private const val ROUTE_VERIFY_ARCULUS_PIN_SCREEN = "$PATH_VERIFY_ARCULUS_PIN_SCREEN/{$ARG_FACTOR_SOURCE_ID}"

internal class VerifyArculusPinArgs(val factorSourceId: FactorSourceId) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        FactorSourceId.fromJson(checkNotNull(savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID)))
    )
}

fun NavController.verifyArculusPin(factorSourceId: FactorSourceId) {
    navigate("${PATH_VERIFY_ARCULUS_PIN_SCREEN}/${factorSourceId.toJson()}")
}

fun NavGraphBuilder.verifyArculusPin(
    onDismiss: () -> Unit,
    onComplete: (FactorSourceId, String) -> Unit
) {
    composable(
        route = ROUTE_VERIFY_ARCULUS_PIN_SCREEN,
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
        VerifyArculusPinScreen(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onComplete = onComplete
        )
    }
}
