package com.babylon.wallet.android.presentation.selectfactorsource

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.radixdlt.sargon.FactorSourceId

private const val ROUTE_SELECT_FACTOR_SOURCE = "select_factor_source"

fun NavController.selectFactorSource() {
    navigate(ROUTE_SELECT_FACTOR_SOURCE)
}

fun NavGraphBuilder.selectFactorSource(
    onDismiss: () -> Unit,
    onComplete: (FactorSourceId) -> Unit
) {
    composable(
        route = ROUTE_SELECT_FACTOR_SOURCE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SelectFactorSourceScreen(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onComplete = onComplete
        )
    }
}
