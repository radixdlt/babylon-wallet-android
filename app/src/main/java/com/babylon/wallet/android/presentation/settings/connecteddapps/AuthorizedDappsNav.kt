package com.babylon.wallet.android.presentation.settings.connecteddapps

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

fun NavController.authorizedDappsScreen() {
    navigate("settings_authorized_dapps")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.authorizedDappsScreen(
    onBackClick: () -> Unit,
    onDappClick: (String) -> Unit
) {
    composable(
        route = "settings_authorized_dapps",
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        AuthorizedDappsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
            onDappClick = onDappClick
        )
    }
}
