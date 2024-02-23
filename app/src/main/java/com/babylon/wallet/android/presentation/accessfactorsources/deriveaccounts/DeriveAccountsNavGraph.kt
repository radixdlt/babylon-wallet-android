package com.babylon.wallet.android.presentation.accessfactorsources.deriveaccounts

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

fun NavController.deriveAccounts() {
    navigate("derive_accounts_bottom_sheet")
}

fun NavGraphBuilder.deriveAccounts(
    onDismiss: () -> Unit
) {
    dialog(
        route = "derive_accounts_bottom_sheet",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DeriveAccountsDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
