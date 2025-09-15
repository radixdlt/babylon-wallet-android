package com.babylon.wallet.android.presentation.addfactorsource.intro

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.addfactorsource.identify.identifyFactorSource
import com.babylon.wallet.android.presentation.addfactorsource.seedphrase.seedPhrase
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.linkedconnectors.intro.linkConnectorIntro

const val ROUTE_ADD_FACTOR_SOURCE_INTRO = "add_factor_source_intro"

fun NavGraphBuilder.addFactorSourceIntro(
    navController: NavController
) {
    composable(
        route = ROUTE_ADD_FACTOR_SOURCE_INTRO,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down) }
    ) {
        AddFactorSourceIntroScreen(
            viewModel = hiltViewModel(),
            onDismiss = navController::popBackStack,
            onInfoClick = { item -> navController.infoDialog(item) },
            onAddDeviceFactorSource = navController::seedPhrase,
            onAddLedgerFactorSource = navController::identifyFactorSource,
            onAddLinkConnector = navController::linkConnectorIntro,
            onAddArculusFactorSource = navController::identifyFactorSource
        )
    }
}
