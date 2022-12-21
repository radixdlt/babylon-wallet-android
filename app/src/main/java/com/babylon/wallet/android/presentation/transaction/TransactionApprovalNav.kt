package com.babylon.wallet.android.presentation.transaction

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.babylon.wallet.android.presentation.navigation.Screen
import com.google.accompanist.navigation.animation.composable

fun NavController.transactionApproval() {
    navigate("transaction_approval_route")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.transactionApprovalScreen(onBackClick: () -> Unit) {
    composable(
        route = Screen.TransactionApprovalDestination.route,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        TransactionApprovalScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick,
        )
    }
}