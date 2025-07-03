package com.babylon.wallet.android.presentation.addfactorsource.intro

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.account.createaccount.withledger.chooseLedger
import com.babylon.wallet.android.presentation.addfactorsource.device.seedphrase.deviceSeedPhrase

const val ROUTE_ADD_FACTOR_INTRO = "add_factor_intro"

fun NavController.addFactorIntro() {
    navigate(ROUTE_ADD_FACTOR_INTRO)
}

fun NavGraphBuilder.addFactorIntro(
    navController: NavController
) {
    composable(
        route = ROUTE_ADD_FACTOR_INTRO,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        AddFactorSourceIntroScreen(
            viewModel = hiltViewModel(),
            onDismiss = navController::popBackStack,
            onAddDeviceFactorSource = navController::deviceSeedPhrase,
            onAddLedgerFactorSource = navController::chooseLedger
        )
    }
}
