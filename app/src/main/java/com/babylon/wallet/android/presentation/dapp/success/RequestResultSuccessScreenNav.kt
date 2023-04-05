package com.babylon.wallet.android.presentation.dapp.success

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.R

@VisibleForTesting
internal const val ARG_REQUEST_ID = "arg_request_id"

@VisibleForTesting
internal const val ARG_DAPP_NAME = "arg_dapp_name"

fun NavController.requestResultSuccess(
    requestId: String,
    dAppName: String
) {
    val name = dAppName.ifEmpty {
        context.resources.getString(R.string.unknown_dapp)
    }
    navigate("request_result_success/$requestId/$name")
}

fun NavGraphBuilder.requestResultSuccess(
    onBackPress: () -> Unit
) {
    dialog(
        route = "request_result_success/{$ARG_REQUEST_ID}/{$ARG_DAPP_NAME}",
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType },
            navArgument(ARG_DAPP_NAME) { type = NavType.StringType }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val requestId = checkNotNull(it.arguments?.getString(ARG_REQUEST_ID))
        val dAppName = checkNotNull(it.arguments?.getString(ARG_DAPP_NAME))

        RequestResultSuccessScreen(
            viewModel = hiltViewModel(),
            requestId = requestId,
            dAppName = dAppName,
            onBackPress = onBackPress
        )
    }
}
