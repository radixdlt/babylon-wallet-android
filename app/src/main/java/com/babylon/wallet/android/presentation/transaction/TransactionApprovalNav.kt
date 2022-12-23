package com.babylon.wallet.android.presentation.transaction

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.dialog
import com.babylon.wallet.android.presentation.navigation.Screen

fun NavController.transactionApproval() {
    navigate("transaction_approval_route")
}

@OptIn(ExperimentalComposeUiApi::class)
fun NavGraphBuilder.transactionApprovalScreen(onBackClick: () -> Unit) {
    dialog(
        route = Screen.TransactionApprovalDestination.route,
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
