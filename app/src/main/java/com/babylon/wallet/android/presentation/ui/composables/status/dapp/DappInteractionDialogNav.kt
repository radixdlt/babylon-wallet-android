package com.babylon.wallet.android.presentation.ui.composables.status.dapp

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.babylon.wallet.android.R
import com.babylon.wallet.android.utils.AppEvent

@VisibleForTesting
private const val ARG_REQUEST_ID = "arg_request_id"

@VisibleForTesting
internal const val ARG_DAPP_NAME = "arg_dapp_name"

internal class DappInteractionSuccessDialogArgs(
    val requestId: String,
    val dAppName: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        requestId = checkNotNull(savedStateHandle.get<String>(ARG_REQUEST_ID)),
        dAppName = checkNotNull(savedStateHandle.get<String>(ARG_DAPP_NAME))
    )
}

fun NavController.dappInteractionDialog(
    event: AppEvent.Status.DappInteraction
) {
    val name = if (event.dAppName.isNullOrBlank()) {
        context.resources.getString(R.string.dAppRequest_metadata_unknownName)
    } else {
        event.dAppName
    }
    navigate("dapp_interaction_dialog/${event.requestId}/$name")
}

fun NavGraphBuilder.dappInteractionDialog(
    onBackPress: () -> Unit
) {
    dialog(
        route = "dapp_interaction_dialog/{$ARG_REQUEST_ID}/{$ARG_DAPP_NAME}",
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) {
                type = NavType.StringType
            },
            navArgument(ARG_DAPP_NAME) {
                type = NavType.StringType
            }
        ),
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        DappInteractionDialog(
            viewModel = hiltViewModel(),
            onBackPress = onBackPress,
        )
    }
}
