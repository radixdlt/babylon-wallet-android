package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

fun NavController.settingsImportOlympiaAccounts() {
    navigate("settings_import_olympia_account")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsImportOlympiaAccounts(
    onBackClick: () -> Unit,
    onAddP2PLink: () -> Unit
) {
    composable(
        route = "settings_import_olympia_account",
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        OlympiaImportScreen(
            viewModel = hiltViewModel(),
            onCloseScreen = onBackClick,
            onAddP2PLink = onAddP2PLink
        )
    }
}
