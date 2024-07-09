package com.babylon.wallet.android.presentation.accessfactorsources.createpersona

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

fun NavController.createPersonaDialog() {
    navigate("create_persona_bottom_sheet")
}

fun NavGraphBuilder.createPersonaDialog(
    onDismiss: () -> Unit
) {
    markAsHighPriority("create_persona_bottom_sheet")
    dialog(
        route = "create_persona_bottom_sheet",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CreatePersonaDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
