package com.babylon.wallet.android.presentation.transaction

import androidx.annotation.VisibleForTesting
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.domain.model.resources.Resource
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

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

fun NavGraphBuilder.transactionReviewScreen(
    onBackClick: () -> Unit,
    onFungibleClick: (Resource.FungibleResource, Boolean) -> Unit,
    onNonFungibleClick: (Resource.NonFungibleResource, Resource.NonFungibleResource.Item, Boolean, Boolean) -> Unit
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
            onDismiss = onBackClick,
            onFungibleClick = onFungibleClick,
            onNonFungibleClick = onNonFungibleClick
        )
    }
}
