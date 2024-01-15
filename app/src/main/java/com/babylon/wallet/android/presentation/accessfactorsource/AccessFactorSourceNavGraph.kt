package com.babylon.wallet.android.presentation.accessfactorsource

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

fun NavController.accessFactorSourceBottomSheet() {
    navigate("access_factor_source_bottom_sheet")
}

fun NavGraphBuilder.accessFactorSourceBottomSheet(
    onDismiss: () -> Unit
) {
    dialog(
        route = "access_factor_source_bottom_sheet",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AccessFactorSourceBottomSheet(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
