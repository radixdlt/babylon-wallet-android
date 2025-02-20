package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.confirmseedphrase

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE_CONFIRM_DEVICE_SEED_PHRASE = "confirm_device_seed_phrase"

fun NavController.confirmDeviceSeedPhrase() {
    navigate(ROUTE_CONFIRM_DEVICE_SEED_PHRASE)
}

fun NavGraphBuilder.confirmDeviceSeedPhrase(
    navController: NavController
) {
    composable(
        route = ROUTE_CONFIRM_DEVICE_SEED_PHRASE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        ConfirmDeviceSeedPhraseScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
        )
    }
}
