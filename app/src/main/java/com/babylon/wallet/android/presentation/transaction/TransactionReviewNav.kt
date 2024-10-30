package com.babylon.wallet.android.presentation.transaction

import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.domain.model.TransferableAsset
import com.babylon.wallet.android.presentation.dialogs.info.GlossaryItem
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority
import com.babylon.wallet.android.presentation.transaction.model.TransferableX
import rdx.works.core.domain.DApp
import rdx.works.core.domain.resources.Resource

@VisibleForTesting
internal const val ARG_TRANSACTION_REQUEST_ID = "arg_transaction_request_id"

const val ROUTE_TRANSACTION_REVIEW = "transaction_review_route/{$ARG_TRANSACTION_REQUEST_ID}"

internal class TransactionReviewArgs(val interactionId: String) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        checkNotNull(savedStateHandle[ARG_TRANSACTION_REQUEST_ID]) as String
    )
}

fun NavController.transactionReview(requestId: String, navOptionsBuilder: NavOptionsBuilder.() -> Unit = {}) {
    navigate("transaction_review_route/$requestId", navOptionsBuilder)
}

fun NavGraphBuilder.transactionReviewScreen(
    onBackClick: () -> Unit,
    onTransferableFungibleClick: (TransferableX.FungibleType) -> Unit,
    onTransferableNonFungibleClick: (TransferableX.NonFungibleType, Resource.NonFungibleResource.Item?) -> Unit,
    onDAppClick: (DApp) -> Unit,
    onInfoClick: (GlossaryItem) -> Unit
) {
    markAsHighPriority(ROUTE_TRANSACTION_REVIEW)
    composable(
        route = ROUTE_TRANSACTION_REVIEW,
        arguments = listOf(
            navArgument(ARG_TRANSACTION_REQUEST_ID) { type = NavType.StringType }
        ),
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up)
        },
        exitTransition = {
            ExitTransition.None
        },
        popEnterTransition = {
            EnterTransition.None
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down)
        }
    ) {
        TransactionReviewScreen(
            viewModel = hiltViewModel(),
            onDismiss = onBackClick,
            onTransferableFungibleClick = onTransferableFungibleClick,
            onTransferableNonFungibleClick = onTransferableNonFungibleClick,
            onDAppClick = onDAppClick,
            onInfoClick = onInfoClick
        )
    }
}
