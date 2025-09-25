package com.babylon.wallet.android.presentation.nfc

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ROUTE_NFC_SESSION_SHEET = "nfc_session_sheet"

fun NavController.nfcDialog() {
    navigate(ROUTE_NFC_SESSION_SHEET)
}

fun NavGraphBuilder.nfcDialog(
    onDismiss: () -> Unit
) {
    markAsHighPriority(ROUTE_NFC_SESSION_SHEET)
    dialog(
        route = ROUTE_NFC_SESSION_SHEET,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        NfcDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
