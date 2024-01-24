package com.babylon.wallet.android.presentation.accessfactorsources

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

fun NavController.accessFactorSources() {
    navigate("access_factor_source_bottom_sheet")
}

fun NavGraphBuilder.accessFactorSources(
    onDismiss: () -> Unit
) {
    dialog(
        route = "access_factor_source_bottom_sheet",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AccessFactorSourcesDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
