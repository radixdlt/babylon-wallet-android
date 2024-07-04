package com.babylon.wallet.android.presentation.accessfactorsources.signatures

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

fun NavController.getSignatures() {
    navigate("get_signatures_bottom_sheet")
}

fun NavGraphBuilder.getSignatures(
    onDismiss: () -> Unit
) {
    dialog(
        route = "get_signatures_bottom_sheet",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        GetSignaturesDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
