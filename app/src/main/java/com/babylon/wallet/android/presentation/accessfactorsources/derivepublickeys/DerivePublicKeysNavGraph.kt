package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickeys

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ROUTE = "derive_public_keys_sheet"

fun NavController.derivePublicKeysDialog() {
    navigate(ROUTE)
}

fun NavGraphBuilder.derivePublicKeysDialog(
    onDismiss: () -> Unit
) {
    markAsHighPriority(ROUTE)
    dialog(
        route = ROUTE,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DerivePublicKeysDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}
