package com.babylon.wallet.android.presentation.addfactorsource.device.seedphrase

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.addfactorsource.ROUTE_ADD_FACTOR_SOURCE_GRAPH
import com.babylon.wallet.android.presentation.addfactorsource.device.confirmseedphrase.confirmDeviceSeedPhrase
import com.babylon.wallet.android.presentation.addfactorsource.kind.ROUTE_ADD_FACTOR_SOURCE_KIND
import com.babylon.wallet.android.utils.routeExist

private const val ROUTE_DEVICE_SEED_PHRASE = "device_seed_phrase"

fun NavController.deviceSeedPhrase() {
    navigate(ROUTE_DEVICE_SEED_PHRASE)
}

fun NavGraphBuilder.deviceSeedPhrase(
    navController: NavController
) {
    composable(
        route = ROUTE_DEVICE_SEED_PHRASE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        DeviceSeedPhraseScreen(
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
            onConfirmed = { navController.confirmDeviceSeedPhrase() }
        )
    }
}
