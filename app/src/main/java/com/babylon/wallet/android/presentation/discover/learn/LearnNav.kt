package com.babylon.wallet.android.presentation.discover.learn

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

const val ROUTE_LEARN = "route_learn"

fun NavController.learnScreen() {
    navigate(route = ROUTE_LEARN)
}

fun NavGraphBuilder.learnScreen(
    onBackClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    composable(
        route = ROUTE_LEARN,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left)
        },
        exitTransition = {
            ExitTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        }
    ) {
        LearnScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onInfoClick = onInfoClick
        )
    }
}
