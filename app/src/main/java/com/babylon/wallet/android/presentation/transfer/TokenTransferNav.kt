package com.babylon.wallet.android.presentation.transfer

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import com.google.accompanist.navigation.animation.composable

private const val ROUTE_TOKEN_TRANSFER = "token_transfer_route"

fun NavController.tokenTransferScreen() {
    navigate(ROUTE_TOKEN_TRANSFER)
}

@OptIn(ExperimentalAnimationApi::class)
fun NavGraphBuilder.tokenTransferScreen(
    onBackClick: () -> Unit
) {
    composable(
        route = ROUTE_TOKEN_TRANSFER,
        enterTransition = {
            slideIntoContainer(AnimatedContentScope.SlideDirection.Up)
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentScope.SlideDirection.Down)
        }
    ) {
        TokenTransferScreen(
            viewModel = hiltViewModel(),
            onBackClick = onBackClick
        )
    }
}
