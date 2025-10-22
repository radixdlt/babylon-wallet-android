package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

private const val ROUTE_GET_SIGNATURES = "get_signatures_bottom_sheet"

fun NavController.getSignatures() {
    navigate(ROUTE_GET_SIGNATURES)
}

fun NavGraphBuilder.getSignatures(
    onDismiss: () -> Unit
) {
    dialog(
        route = ROUTE_GET_SIGNATURES,
        dialogProperties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        GetSignaturesDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
