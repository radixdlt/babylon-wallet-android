package com.babylon.wallet.android.presentation.settings.connecteddapps

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

fun NavController.connectedDappsScreen() {
    navigate("settings_connected_dapps")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.connectedDappsScreen(
    onBackClick: () -> Unit,
    onDappClick: (String) -> Unit
) {
    composable(
        route = "settings_connected_dapps",
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            ExitTransition.None
        }
    ) {
        ConnectedDappsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onDappClick = onDappClick
        )
    }
}
