package com.babylon.wallet.android.presentation.accessfactorsources.derivepublickey

import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog

fun NavController.derivePublicKey() {
    navigate("derive_public_key_bottom_sheet")
}

fun NavGraphBuilder.derivePublicKey(
    onDismiss: () -> Unit
) {
    dialog(
        route = "derive_public_key_bottom_sheet",
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DerivePublicKeyDialog(
            viewModel = hiltViewModel(),
            onDismiss = onDismiss
        )
    }
}