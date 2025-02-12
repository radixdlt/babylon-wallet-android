package com.babylon.wallet.android.presentation.accessfactorsources.spotcheck

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ROUTE = "spot_check_sheet"

fun NavController.spotCheck() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.spotCheckDialog(
    onDismiss: () -> Unit
) {
    markAsHighPriority(route = ROUTE)
    dialog(
        route = ROUTE,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SpotCheckDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
