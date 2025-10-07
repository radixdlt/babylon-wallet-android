package com.babylon.wallet.android.presentation.addfactorsource.identify

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

private const val ROUTE = "identify_factor_source_sheet"

fun NavController.identifyFactorSource() {
    navigate(route = ROUTE)
}

fun NavGraphBuilder.identifyFactorSource(
    onDismiss: () -> Unit,
    onLedgerIdentified: () -> Unit,
    onArculusIdentified: () -> Unit
) {
    dialog(
        route = ROUTE,
        dialogProperties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        IdentifyFactorSourceDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss,
            onLedgerIdentified = onLedgerIdentified,
            onArculusIdentified = onArculusIdentified
        )
    }
}
