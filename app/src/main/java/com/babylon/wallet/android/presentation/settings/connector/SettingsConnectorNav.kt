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
internal const val ARG_CONNECTION_PASSWORD = "arg_connection_password"

@VisibleForTesting
internal const val ARG_SCAN_QR = "arg_request_source"

@VisibleForTesting
internal const val ARG_CLOSE_AFTER_LINKED = "arg_close_after_linked"

internal class SettingsConnectorScreenArgs(
    val scanQr: Boolean,
    val closeAfterLinked: Boolean = false,
    val connectionPassword: String? = null
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_SCAN_QR]) as Boolean,
        checkNotNull(savedStateHandle[ARG_CLOSE_AFTER_LINKED]) as Boolean,
        savedStateHandle.get<String>(ARG_CONNECTION_PASSWORD)
    )
}

// TODO https://github.com/radixdlt/babylon-wallet-android/pull/303#discussion_r1233727181
fun NavController.settingsConnectorScreen(
    scanQr: Boolean = false,
    closeAfterLinked: Boolean = false,
    connectionPassword: String? = null
) {
    var route = "settings_add_connector_route?$ARG_SCAN_QR=$scanQr&$ARG_CLOSE_AFTER_LINKED=$closeAfterLinked"
    connectionPassword?.let {
        route += "&$ARG_CONNECTION_PASSWORD=$it"
    }
    navigate(route)
}

const val ROUTE = "settings_add_connector_route" +
    "?$ARG_SCAN_QR={$ARG_SCAN_QR}" +
    "&$ARG_CLOSE_AFTER_LINKED={$ARG_CLOSE_AFTER_LINKED}" +
    "&$ARG_CONNECTION_PASSWORD={$ARG_CONNECTION_PASSWORD}"

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.settingsConnectorScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE,
        arguments = listOf(
            navArgument(ARG_SCAN_QR) { type = NavType.BoolType },
            navArgument(ARG_CLOSE_AFTER_LINKED) { type = NavType.BoolType },
            navArgument(ARG_CONNECTION_PASSWORD) {
                type = NavType.StringType
                nullable = true
            }
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
