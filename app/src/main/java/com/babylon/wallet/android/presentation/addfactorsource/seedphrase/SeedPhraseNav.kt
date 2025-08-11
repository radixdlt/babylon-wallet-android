package com.babylon.wallet.android.presentation.addfactorsource.seedphrase

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.addfactorsource.ROUTE_ADD_FACTOR_SOURCE_GRAPH
import com.babylon.wallet.android.presentation.addfactorsource.confirmseedphrase.confirmSeedPhrase
import com.babylon.wallet.android.presentation.addfactorsource.kind.ROUTE_ADD_FACTOR_SOURCE_KIND
import com.babylon.wallet.android.utils.routeExist

private const val ROUTE_SEED_PHRASE = "seed_phrase"

fun NavController.seedPhrase() {
    navigate(ROUTE_SEED_PHRASE)
}

fun NavGraphBuilder.seedPhrase(
    navController: NavController
) {
    composable(
        route = ROUTE_SEED_PHRASE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SeedPhraseScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onDismissFlow = {
                val popUpToRoute = if (navController.routeExist(ROUTE_ADD_FACTOR_SOURCE_KIND)) {
                    ROUTE_ADD_FACTOR_SOURCE_KIND
                } else {
                    ROUTE_ADD_FACTOR_SOURCE_GRAPH
                }
                navController.popBackStack(popUpToRoute, true)
            },
            onConfirmed = { navController.confirmSeedPhrase() }
        )
    }
}
