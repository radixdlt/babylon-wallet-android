package com.babylon.wallet.android.presentation.settings.preferences.theme

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

private const val ROUTE_THEME_SELECTION = "theme_selection_bottom_sheet"

fun NavController.themeSelection() {
    navigate(ROUTE_THEME_SELECTION)
}

fun NavGraphBuilder.themeSelection(
    onDismiss: () -> Unit
) {
    dialog(
        route = ROUTE_THEME_SELECTION,
        dialogProperties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        ThemeSelectionDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
