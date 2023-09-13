package com.babylon.wallet.android.presentation.transaction

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.google.accompanist.navigation.animation.composable

@VisibleForTesting
internal const val ARG_TRANSACTION_REQUEST_ID = "arg_transaction_request_id"

const val ROUTE_TRANSACTION_REVIEW = "transaction_review_route/{$ARG_TRANSACTION_REQUEST_ID}"

internal class TransactionReviewArgs(val requestId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_TRANSACTION_REQUEST_ID]) as String
    )
}

fun NavController.transactionReview(requestId: String) {
    navigate("transaction_review_route/$requestId")
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.transactionReviewScreen(
    onBackClick: () -> Unit
) {
    markAsHighPriority(ROUTE_TRANSACTION_REVIEW)
    composable(
        route = ROUTE_TRANSACTION_REVIEW,
        arguments = listOf(
            navArgument(ARG_TRANSACTION_REQUEST_ID) { type = NavType.StringType }
        )
    ) {
        TransactionReviewScreen(
            viewModel = hiltViewModel(),
            onDismiss = onBackClick
        )
    }
}
