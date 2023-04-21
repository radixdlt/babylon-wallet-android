package com.babylon.wallet.android.presentation.ui.composables.resultdialog.failed

import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

@VisibleForTesting
private const val ARG_REQUEST_ID = "arg_request_id"

@VisibleForTesting
internal const val ARG_ERROR_TEXT = "arg_error_text"

fun NavController.failedBottomDialog(
    requestId: String,
    @StringRes errorTextRes: Int
) {
    val errorText = context.resources.getString(errorTextRes)
    navigate("failed_bottom_dialog/$requestId/$errorText")
}

fun NavGraphBuilder.failedBottomDialog(
    onBackPress: () -> Unit
) {
    dialog(
        route = "failed_bottom_dialog/{$ARG_REQUEST_ID}/{$ARG_ERROR_TEXT}",
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_ERROR_TEXT) {
                type = NavType.StringType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val requestId = checkNotNull(it.arguments?.getString(ARG_REQUEST_ID))
        val errorText = it.arguments?.getString(ARG_ERROR_TEXT).orEmpty()

        FailedBottomDialog(
            viewModel = hiltViewModel(),
            requestId = requestId,
            errorText = errorText,
            onBackPress = onBackPress
        )
    }
}
