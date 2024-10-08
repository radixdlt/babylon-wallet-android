package com.babylon.wallet.android.presentation.mobileconnect

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.babylon.wallet.android.presentation.navigation.markAsHighPriority

private const val ARG_INTERACTION_ID = "interactionId"
const val ROUTE_MOBILE_CONNECT = "mobileConnect/{$ARG_INTERACTION_ID}"

fun NavController.mobileConnect(
    interactionId: String
) {
    navigate(route = "mobileConnect/$interactionId")
}

internal class MobileConnectArgs(
    val interactionId: String
) {
    constructor(savedStateHandle: SavedStateHandle) : this(
        interactionId = checkNotNull(savedStateHandle.get<String>(ARG_INTERACTION_ID))
    )
}

fun NavGraphBuilder.mobileConnect(
    onBackClick: () -> Unit,
    onHandleRequestAuthorizedRequest: (String) -> Unit,
    onHandleUnauthorizedRequest: (String) -> Unit,
    onHandleTransactionRequest: (String) -> Unit
) {
    markAsHighPriority(route = ROUTE_MOBILE_CONNECT)
    composable(
        route = ROUTE_MOBILE_CONNECT,
        arguments = listOf(
            navArgument(ARG_INTERACTION_ID) {
                type = NavType.StringType
            }
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
        MobileConnectLinkScreen(
            onClose = onBackClick,
            onHandleRequestAuthorizedRequest = onHandleRequestAuthorizedRequest,
            onHandleUnauthorizedRequest = onHandleUnauthorizedRequest,
            onHandleTransactionRequest = onHandleTransactionRequest
        )
    }
}
