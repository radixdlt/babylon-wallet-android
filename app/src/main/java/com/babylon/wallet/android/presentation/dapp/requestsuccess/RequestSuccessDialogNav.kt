package com.babylon.wallet.android.presentation.dapp.requestsuccess

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

@VisibleForTesting
internal const val ARG_DAPP_NAME = "dapp_name"

fun NavController.requestSuccess(dappName: String) {
    navigate("request_success/$dappName")
}

fun NavGraphBuilder.requestSuccess(onBackPress: () -> Unit) {
    dialog(
        route = "request_success/{$ARG_DAPP_NAME}",
        arguments = listOf(
            navArgument(ARG_DAPP_NAME) { type = NavType.StringType },
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val dappName = checkNotNull(it.arguments?.getString(ARG_DAPP_NAME))
        RequestSuccessDialog(dappName = dappName, onBackPress)
    }
}
