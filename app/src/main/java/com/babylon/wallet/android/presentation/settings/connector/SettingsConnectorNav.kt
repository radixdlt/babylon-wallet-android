package com.babylon.wallet.android.presentation.settings.connector

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_SCAN_QR = "arg_request_source"

internal class SettingsConnectorScreenArgs(val scanQr: Boolean) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_SCAN_QR]) as Boolean
    )
}

fun NavController.settingsConnectorScreen(scanQr: Boolean = false) {
    navigate("settings_add_connector_route/$scanQr")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsConnectorScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = "settings_add_connector_route/{$ARG_SCAN_QR}",
        arguments = listOf(
            navArgument(ARG_SCAN_QR) { type = NavType.BoolType },
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Left)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Right)
        }
    ) {
        SettingsConnectorScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
