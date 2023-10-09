package com.babylon.wallet.android.presentation.settings.accountsecurity.depositguarantees

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

private const val ROUTE = "deposit_guarantees_route"

fun NavController.depositGuaranteesScreen() {
    navigate(ROUTE) {
        launchSingleTop = true
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.depositGuaranteesScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        DepositGuaranteesScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
