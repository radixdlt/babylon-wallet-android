package com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.intro

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.infoDialog
import com.babylon.wallet.android.presentation.settings.securitycenter.addfactor.device.seedphrase.deviceSeedPhrase

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
        AddFactorIntroScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onContinueClick = { navController.deviceSeedPhrase() },
            onInfoClick = { item -> navController.infoDialog(item) }
        )
    }
}
