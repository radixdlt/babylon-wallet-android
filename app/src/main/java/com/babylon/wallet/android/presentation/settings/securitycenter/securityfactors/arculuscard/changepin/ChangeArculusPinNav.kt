package com.babylon.wallet.android.presentation.settings.securitycenter.securityfactors.arculuscard.changepin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.radixdlt.sargon.FactorSourceId
import com.radixdlt.sargon.extensions.fromJson
import com.radixdlt.sargon.extensions.toJson

private const val ARG_FACTOR_SOURCE_ID = "factor_source_id"
private const val ARG_OLD_PIN = "old_pin"

private const val PATH_CHANGE_ARCULUS_PIN_SCREEN = "route_change_arculus_pin"
private const val ROUTE_CHANGE_ARCULUS_PIN_SCREEN = PATH_CHANGE_ARCULUS_PIN_SCREEN +
    "?$ARG_FACTOR_SOURCE_ID={$ARG_FACTOR_SOURCE_ID}" +
    "&$ARG_OLD_PIN={$ARG_OLD_PIN}"

internal class ChangeArculusPinArgs(
    val factorSourceId: FactorSourceId,
    val oldPin: String
) {

    constructor(savedStateHandle: SavedStateHandle) : this(
        FactorSourceId.fromJson(checkNotNull(savedStateHandle.get<String>(ARG_FACTOR_SOURCE_ID))),
        checkNotNull(savedStateHandle.get<String>(ARG_OLD_PIN))
    )
}

fun NavController.changeArculusPin(
    factorSourceId: FactorSourceId,
    oldPin: String,
    navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}
) {
    navigate(
        route = PATH_CHANGE_ARCULUS_PIN_SCREEN +
            "?$ARG_FACTOR_SOURCE_ID=${factorSourceId.toJson()}" +
            "&$ARG_OLD_PIN=$oldPin",
        builder = navOptionsBuilder
    )
}

fun NavGraphBuilder.changeArculusPin(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    composable(
        route = ROUTE_CHANGE_ARCULUS_PIN_SCREEN,
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
            },
            navArgument(ARG_OLD_PIN) {
                type = NavType.StringType
            }
        )
    ) {
        ChangeArculusPinScreen(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onComplete = onComplete
        )
    }
}
