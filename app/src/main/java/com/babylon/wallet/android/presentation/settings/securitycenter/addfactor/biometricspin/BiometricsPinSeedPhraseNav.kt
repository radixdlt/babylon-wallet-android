package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.biometricspin

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val ROUTE_BIOMETRICS_PIN_SEED_PHRASE = "biometrics_pin_seed_phrase"

fun NavController.biometricsPinSeedPhrase() {
    navigate(ROUTE_BIOMETRICS_PIN_SEED_PHRASE)
}

fun NavGraphBuilder.biometricsPinSeedPhrase(
    navController: NavController
) {
    composable(
        route = ROUTE_BIOMETRICS_PIN_SEED_PHRASE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        BiometricsPinSeedPhraseScreen(
            onDismiss = { navController.popBackStack() },
        )
    }
}
