package com.babylon.wallet.android.presentation.addfactorsource.confirmseedphrase

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.addfactorsource.arculus.createpin.createArculusPin
import com.babylon.wallet.android.presentation.addfactorsource.name.setFactorSourceName

private const val ROUTE_CONFIRM_SEED_PHRASE = "confirm_seed_phrase"

fun NavController.confirmSeedPhrase() {
    navigate(ROUTE_CONFIRM_SEED_PHRASE)
}

fun NavGraphBuilder.confirmSeedPhrase(
    navController: NavController
) {
    composable(
        route = ROUTE_CONFIRM_SEED_PHRASE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        ConfirmSeedPhraseScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onDeviceSeedPhraseConfirmed = { navController.setFactorSourceName() },
            onArculusSeedPhraseConfirmed = { navController.createArculusPin() }
        )
    }
}
