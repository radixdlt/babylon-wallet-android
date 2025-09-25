package com.babylon.wallet.android.presentation.accessfactorsources.authorization

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ROUTE_REQUEST_AUTHORIZATION = "request_authorization_bottom_sheet"

fun NavController.requestAuthorization() {
    navigate(route = ROUTE_REQUEST_AUTHORIZATION)
}

fun NavGraphBuilder.requestAuthorizationDialog(
    onDismiss: () -> Unit
) {
    markAsHighPriority(ROUTE_REQUEST_AUTHORIZATION)
    dialog(
        route = ROUTE_REQUEST_AUTHORIZATION,
        dialogProperties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        RequestAuthorizationDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
