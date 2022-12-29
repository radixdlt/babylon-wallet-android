package com.babylon.wallet.android.presentation.transaction

import androidx.annotation.VisibleForTesting
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

@VisibleForTesting
internal const val ARG_REQUEST_ID = "request_id"

internal class TransactionApprovalArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(checkNotNull(savedStateHandle[ARG_REQUEST_ID]) as String)
}

fun NavController.transactionApproval(requestId: String) {
    navigate("transaction_approval_route/$requestId")
}

@OptIn(ExperimentalComposeUiApi::class)
fun NavGraphBuilder.transactionApprovalScreen(onBackClick: () -> Unit) {
    dialog(
        route = "transaction_approval_route/{$ARG_REQUEST_ID}",
        arguments = listOf(
            navArgument(ARG_REQUEST_ID) { type = NavType.StringType }
        ),
        dialogProperties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
//        enterTransition = {
//            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
//        },
//        exitTransition = {
//            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
//        }
    ) {
        TransactionApprovalScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}
