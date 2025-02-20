package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.seedphrase

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.confirmseedphrase.confirmDeviceSeedPhrase

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
            onConfirmed = { navController.confirmDeviceSeedPhrase() }
        )
    }
}
