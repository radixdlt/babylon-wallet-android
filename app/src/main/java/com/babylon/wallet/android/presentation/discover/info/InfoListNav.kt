package com.babylon.wallet.android.presentation.discover.info

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem

const val ROUTE_INFO_LIST = "route_info_list"

fun NavController.infoListScreen() {
    navigate(route = ROUTE_INFO_LIST)
}

fun NavGraphBuilder.infoListScreen(
    onBackClick: () -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    composable(
        route = ROUTE_INFO_LIST,
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
        InfoListScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onInfoClick = onInfoClick
        )
    }
}