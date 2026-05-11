package com.babylon.wallet.android.presentation.settings.preferences.tokenpriceservices

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

private const val ROUTE = "token_price_services_route"

fun NavController.tokenPriceServices() {
    navigate(ROUTE)
}

fun NavGraphBuilder.tokenPriceServices(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left) },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right) }
    ) {
        TokenPriceServicesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
