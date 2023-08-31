package com.babylon.wallet.android.presentation.settings.appsettings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

fun NavController.appSettingsScreen() {
    navigate("settings_app_settings") {
        launchSingleTop = true
    }
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.appSettingsScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = "settings_app_settings",
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        AppSettingsScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
