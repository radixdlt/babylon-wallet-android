package com.babylon.wallet.android.presentation.settings.seedphrase

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

const val ROUTE_SETTINGS_SHOW_MNEMONIC = "settings_show_mnemonic"

fun NavController.settingsShowMnemonic() {
    navigate(ROUTE_SETTINGS_SHOW_MNEMONIC)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsShowMnemonic(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_SETTINGS_SHOW_MNEMONIC,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        ShowMnemonicScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
