package com.babylon.wallet.android.presentation.ui.composables.resultdialog.success

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
internal const val ARG_IS_FROM_TRANSACTION = "arg_is_from_transaction"

@VisibleForTesting
private const val ARG_REQUEST_ID = "arg_request_id"

@VisibleForTesting
internal const val ARG_DAPP_NAME = "arg_dapp_name"

fun NavController.successBottomDialog(
    isFromTransaction: Boolean,
    requestId: String,
    dAppName: String? = null
) {
    val name = dAppName?.ifEmpty {
        context.resources.getString(R.string.dAppRequest_metadata_unknownName)
    }
    navigate("success_bottom_dialog/$isFromTransaction/$requestId/$name")
}

fun NavGraphBuilder.successBottomDialog(
    onBackPress: () -> Unit
) {
    dialog(
        route = "success_bottom_dialog/{$ARG_IS_FROM_TRANSACTION}/{$ARG_REQUEST_ID}/{$ARG_DAPP_NAME}",
        arguments = listOf(
            navArgument(ARG_IS_FROM_TRANSACTION) {
                type = NavType.BoolType
                defaultValue = false
            },
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_DAPP_NAME) {
                type = NavType.StringType
                nullable = true
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val isFromTransaction = it.arguments?.getBoolean(ARG_IS_FROM_TRANSACTION) ?: false
        val requestId = checkNotNull(it.arguments?.getString(ARG_REQUEST_ID))
        val dAppName = it.arguments?.getString(ARG_DAPP_NAME)

        SuccessBottomDialog(
            viewModel = hiltViewModel(),
            requestId = requestId,
            isFromTransaction = isFromTransaction,
            dAppName = dAppName.orEmpty(), // orEmpty because transaction requested the dialog
            onBackPress = onBackPress
        )
    }
}
