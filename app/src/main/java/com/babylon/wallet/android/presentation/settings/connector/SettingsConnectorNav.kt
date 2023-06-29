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

@VisibleForTesting
internal const val ARG_CLOSE_AFTER_LINKED = "arg_close_after_linked"

internal class SettingsConnectorScreenArgs(val scanQr: Boolean, val closeAfterLinked: Boolean = false) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_SCAN_QR]) as Boolean,
        checkNotNull(savedStateHandle[ARG_CLOSE_AFTER_LINKED]) as Boolean
    )
}

// TODO https://github.com/radixdlt/babylon-wallet-android/pull/303#discussion_r1233727181
fun NavController.settingsConnectorScreen(scanQr: Boolean = false, closeAfterLinked: Boolean = false) {
    navigate("settings_add_connector_route/$scanQr/$closeAfterLinked")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsConnectorScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = "settings_add_connector_route/{$ARG_SCAN_QR}/{$ARG_CLOSE_AFTER_LINKED}",
        arguments = listOf(
            navArgument(ARG_SCAN_QR) { type = NavType.BoolType },
            navArgument(ARG_CLOSE_AFTER_LINKED) { type = NavType.BoolType },
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
