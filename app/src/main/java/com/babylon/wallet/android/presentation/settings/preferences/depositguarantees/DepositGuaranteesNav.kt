package com.babylon.wallet.android.presentation.settings.preferences.depositguarantees

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

private const val ROUTE = "deposit_guarantees_route"

fun NavController.depositGuaranteesScreen() {
    navigate(ROUTE) {
        launchSingleTop = true
    }
}

fun NavGraphBuilder.depositGuaranteesScreen(
    onInfoClick: (GlossaryItem) -> Unit,
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        }
    ) {
        DepositGuaranteesScreen(
            viewModel = hiltViewModel(),
            onInfoClick = onInfoClick,
            onBackClick = onBackClick
        )
    }
}
