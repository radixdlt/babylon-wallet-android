package com.babylon.wallet.android.presentation.addfactorsource.name

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.addfactorsource.intro.ROUTE_ADD_FACTOR_SOURCE_INTRO

const val ROUTE_SET_FACTOR_SOURCE_NAME = "set_factor_source_name"

fun NavController.setFactorSourceName() {
    navigate(ROUTE_SET_FACTOR_SOURCE_NAME)
}

fun NavGraphBuilder.setFactorSourceName(
    navController: NavController
) {
    composable(
        route = ROUTE_SET_FACTOR_SOURCE_NAME,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        SetFactorSourceNameScreen(
            viewModel = hiltViewModel(),
            onDismiss = { navController.popBackStack() },
            onSaved = { navController.popBackStack(ROUTE_ADD_FACTOR_SOURCE_INTRO, true) }
        )
    }
}
