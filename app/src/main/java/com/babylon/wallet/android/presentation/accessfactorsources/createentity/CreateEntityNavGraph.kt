package com.babylon.wallet.android.presentation.accessfactorsources.createentity

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

fun NavController.createEntityDialog() {
    navigate("create_entity_bottom_sheet")
}

fun NavGraphBuilder.createEntityDialog(
    onDismiss: () -> Unit
) {
    markAsHighPriority("create_entity_bottom_sheet")
    dialog(
        route = "create_entity_bottom_sheet",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CreateEntityDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
