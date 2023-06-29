package com.babylon.wallet.android.presentation.settings.legacyimport

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable

private const val ROUTE = "settings_import_olympia_account"

fun NavController.settingsImportOlympiaAccounts() {
    navigate(ROUTE)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsImportOlympiaAccounts(
    onBackClick: () -> Unit,
    onAddP2PLink: () -> Unit
) {
    markAsHighPriority(ROUTE)
    composable(
        route = ROUTE,
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
